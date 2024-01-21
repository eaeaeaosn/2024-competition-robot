package net.ironpulse.commands;

import com.ctre.phoenix6.controls.MotionMagicVoltage;
import edu.wpi.first.wpilibj2.command.Command;
import net.ironpulse.Constants;
import net.ironpulse.RobotContainer;
import net.ironpulse.subsystems.ShooterSubsystem;

public class AmpAimingCommand extends Command {
    private final ShooterSubsystem shooterSubsystem;
    private final RobotContainer robotContainer;

    public AmpAimingCommand(ShooterSubsystem shooterSubsystem, RobotContainer robotContainer) {
        this.shooterSubsystem = shooterSubsystem;
        this.robotContainer = robotContainer;
        addRequirements(shooterSubsystem);
    }

    @Override
    public void execute() {
        shooterSubsystem.getDeployMotor().setControl(
                new MotionMagicVoltage(Constants.ShooterConstants.ampDeployAngle.magnitude()));
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            robotContainer.getGlobalState().transfer(RobotContainer.Actions.INTERRUPT_SHOOT);
            return;
        }
        robotContainer.getGlobalState().transfer(RobotContainer.Actions.AIM);
    }
}
