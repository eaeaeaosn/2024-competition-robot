package net.ironpulse;

import com.ctre.phoenix6.mechanisms.swerve.SwerveModule;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import lombok.Getter;
import net.ironpulse.Constants.OperatorConstants;
import net.ironpulse.commands.*;
import net.ironpulse.commands.autos.AutoIntakeCommand;
import net.ironpulse.commands.autos.AutoShootCommand;
import net.ironpulse.commands.manuals.*;
import net.ironpulse.maths.MathMisc;
import net.ironpulse.subsystems.*;
import net.ironpulse.telemetries.*;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.Rotations;
import static net.ironpulse.Constants.SwerveConstants.*;

public class RobotContainer {
    @Getter
    private final CommandXboxController driverController =
            new CommandXboxController(OperatorConstants.DRIVER_CONTROLLER_PORT);

    private final CommandXboxController operatorController =
            new CommandXboxController(OperatorConstants.OPERATOR_CONTROLLER_PORT);

    private final IndexerTelemetry indexerTelemetry = new IndexerTelemetry();
    private final SwerveTelemetry swerveTelemetry = new SwerveTelemetry(maxSpeed);
    private final IntakerTelemetry intakerTelemetry = new IntakerTelemetry();
    private final ShooterTelemetry shooterTelemetry = new ShooterTelemetry();
    private final BeamBreakTelemetry beamBreakTelemetry = new BeamBreakTelemetry();

    public final SwerveSubsystem swerveSubsystem = Constants.SwerveConstants.DriveTrain;
    public final IndexerSubsystem indexerSubsystem =
            new IndexerSubsystem(indexerTelemetry::telemeterize);

    @Getter
    public final BeamBreakSubsystem beamBreakSubsystem =
            new BeamBreakSubsystem(this, beamBreakTelemetry::telemeterize);
    private final IntakerSubsystem intakerSubsystem =
            new IntakerSubsystem(intakerTelemetry::telemeterize);
    private final ShooterSubsystem shooterSubsystem =
            new ShooterSubsystem(shooterTelemetry::telemeterize);

    @Getter
    private final IndicatorSubsystem indicatorSubsystem = new IndicatorSubsystem();

    private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
            .withDeadband(maxSpeed.magnitude() * 0.1)
            .withRotationalDeadband(maxAngularRate.magnitude() * 0.1)
            .withSteerRequestType(SwerveModule.SteerRequestType.MotionMagicExpo);
    private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();

    private final SendableChooser<Command> autoChooser =
            AutoBuilder.buildAutoChooser("M 1 Note Auto");

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void configureKeyBindings() {
        var speedX = Math.abs(driverController.getLeftY());
        var directionX = MathMisc.sign(-driverController.getLeftY());
        var speedY = Math.abs(driverController.getLeftX());
        var directionY = MathMisc.sign(-driverController.getLeftX());
        swerveSubsystem.setDefaultCommand(swerveSubsystem
                .applyRequest(() -> drive.withVelocityX(directionX * xLimiter.calculate(speedX) * maxSpeed.magnitude())
                        .withVelocityY(directionY * yLimiter.calculate(speedY) * maxSpeed.magnitude())
                        .withRotationalRate(-driverController.getRightX() * maxAngularRate.magnitude()))
                .ignoringDisable(true));

        driverController.b().whileTrue(swerveSubsystem.applyRequest(() -> brake));

        driverController.start().onTrue(swerveSubsystem.runOnce(swerveSubsystem::seedFieldRelative));
        swerveSubsystem.registerTelemetry(swerveTelemetry::telemeterize);

        operatorController.rightTrigger().whileTrue(new SpeakerShootCommand(this, swerveSubsystem,
                shooterSubsystem, indexerSubsystem, () -> operatorController.getHID().getAButton()));
        operatorController.leftTrigger().whileTrue(new AmpShootCommand(this,
                shooterSubsystem, indexerSubsystem, () -> operatorController.getHID().getAButton()));

        driverController.rightBumper().whileTrue(
                Commands.parallel(
                        new IntakeCommand(this, intakerSubsystem),
                        new IndexCommand(this, indexerSubsystem)
                )
        );

        operatorController.pov(180).whileTrue(new ManualShooterUpCommand(shooterSubsystem));
        operatorController.pov(0).whileTrue(new ManualShooterDownCommand(shooterSubsystem))
                .and(() -> Rotations.of(shooterSubsystem.getArmMotor().getPosition().getValue()).in(Degrees) > 15);
        driverController.leftTrigger().whileTrue(Commands.parallel(
                new ManualIntakeInCommand(intakerSubsystem),
                new ManualIndexInCommand(indexerSubsystem)));
        driverController.rightTrigger().whileTrue(Commands.parallel(
                new ManualIntakeOutCommand(intakerSubsystem),
                new ManualIndexOutCommand(indexerSubsystem)));

        operatorController.rightBumper().onTrue(new ManualShootCommand(shooterSubsystem));

        operatorController.start().onTrue(new ResetArmCommand(shooterSubsystem));
    }

    private void configureAutos() {
        NamedCommands.registerCommand("AutoShoot",
                new AutoShootCommand(shooterSubsystem, indexerSubsystem));
        NamedCommands.registerCommand("Intake",
                new AutoIntakeCommand(intakerSubsystem, indexerSubsystem, beamBreakSubsystem));
        SmartDashboard.putData("Auto Chooser", autoChooser);
    }

    public Command getAutonomousCommand() {
        return autoChooser.getSelected();
    }
    
    public RobotContainer() {
        configureAutos();
        configureKeyBindings();
    }
}
