/**
 * Only lift test
 */

package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.TouchSensor;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;

import org.openftc.revextensions2.*;

// @Config
@com.qualcomm.robotcore.eventloop.opmode.TeleOp(name = "LIFT TEST WITH NEW STAGES", group = "TeleOp")
public class LiftTestWITHNEWSTAGES extends OpMode {
    private static DcMotor liftMotor1;
    private static ExpansionHubMotor liftRE2;
    private static DcMotorEx liftEx1;
    private TouchSensor liftTouch;

    // lift encoders
    static final double LIFT_COUNTS_PER_MOTOR_REV = 537.6;
    static final double LIFT_DRIVE_GEAR_REDUCTION = 1.0;
    static final double LIFT_WHEEL_DIAMETER_INCHES = 1.25;
    static final double LIFT_COUNTS_PER_INCH = (LIFT_COUNTS_PER_MOTOR_REV * LIFT_DRIVE_GEAR_REDUCTION) /
            (LIFT_WHEEL_DIAMETER_INCHES * 3.1415);  // 136.90275

    // lift things
    int currentLiftStage = 0;
    int nextLiftStage = 1;
    int targetPos = 0;
    PIDFCoefficients pidfCoefficients;

    public static double lkp = 6;
    public static double lki = 0;
    public static double lkd = 0;
    public static double lkf = 0;
    public static double liftPower = 1;

    boolean previousGP2LBPos = false;
    boolean previousGP2RBPos = false;
    boolean previousGP2LTPos = false;
    boolean previousGP2RTPos = false;

    boolean currentGP2LBPos = false;
    boolean currentGP2RBPos = false;
    boolean currentGP2LTPos = false;
    boolean currentGP2RTPos = false;

    @Override
    public void init() {
        liftMotor1 = hardwareMap.dcMotor.get("lift motor 1");
        liftRE2 = (ExpansionHubMotor) hardwareMap.dcMotor.get("lift motor 1");
        liftEx1 = (DcMotorEx) hardwareMap.dcMotor.get("lift motor 1");

        // FtcDashboard dashboard = FtcDashboard.getInstance();
        // telemetry = dashboard.getTelemetry();

        pidfCoefficients = new PIDFCoefficients(lkp, lki, lkd, lkf);

        liftTouch = hardwareMap.get(TouchSensor.class, "liftTouch");

        liftEx1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        liftEx1.setDirection(DcMotorSimple.Direction.REVERSE);
    }

    @Override
    public void loop() {
        currentGP2LBPos = gamepad2.left_bumper;
        currentGP2RBPos = gamepad2.right_bumper;
        currentGP2LTPos = gamepad2.left_trigger > 0.5;
        currentGP2RTPos = gamepad2.right_trigger > 0.5;

        // lift - 1 stage above the previous stage
        if (currentGP2RBPos && !previousGP2RBPos) {
            if (nextLiftStage <= 7) {
                currentLiftStage = nextLiftStage;
                targetPos = (int) ((currentLiftStage * 4) * LIFT_COUNTS_PER_INCH);
            }

            previousGP2RBPos = currentGP2RBPos;
        } else {
            previousGP2RBPos = currentGP2RBPos;
        }

        //  lift - completely down
        if (currentGP2LBPos && !previousGP2LBPos) {
            if (currentLiftStage == 0) {
                return;
            }

            currentLiftStage = 0;
            targetPos = 0;
            if (nextLiftStage != 7) {
                nextLiftStage++;
            }

            previousGP2LBPos = currentGP2LBPos;
        } else {
            previousGP2LBPos = currentGP2LBPos;
        }

        // lift - up 1 stage
        if (currentGP2RTPos && !previousGP2RTPos) {
            if (nextLiftStage >= 0 && nextLiftStage < 7 && currentLiftStage != 7) {
                currentLiftStage++;
            }

            previousGP2RTPos = currentGP2RTPos;
            nextLiftStage = currentLiftStage + 1;
            targetPos = (int) ((currentLiftStage * 4) * LIFT_COUNTS_PER_INCH);
        } else {
            previousGP2RTPos = currentGP2RTPos;
        }

        // lift - down 1 stage
        if (currentGP2LTPos && !previousGP2LTPos) {
            if (nextLiftStage > 0 && nextLiftStage <= 7 && currentLiftStage != 0) {
                currentLiftStage--;
            }

            previousGP2LTPos = currentGP2LTPos;
            nextLiftStage = currentLiftStage + 1;
            targetPos = (int) ((currentLiftStage * 4) * LIFT_COUNTS_PER_INCH);
        } else {
            previousGP2LTPos = currentGP2LTPos;
        }

        // LIFT DcMotorEx
        if (currentLiftStage == 0 && !liftTouch.isPressed()) {
            liftEx1.setTargetPosition(targetPos);
            liftEx1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            liftEx1.setPower(-0.3);
        } else if (currentLiftStage == 0 && liftTouch.isPressed()) {
            liftEx1.setPower(0);
            liftEx1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        } else if (currentLiftStage != 0 && liftMotor1.getCurrentPosition() != targetPos) {
            liftEx1.setTargetPosition(targetPos);
            liftEx1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            liftEx1.setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION, pidfCoefficients);
            liftEx1.setPower(liftPower);
        }

        telemetry.addData("liftTouchSensor", liftTouch.isPressed());
        telemetry.addData("lift1 encoder count", liftMotor1.getCurrentPosition());
        telemetry.addData("lift1 current", liftRE2.getCurrentDraw(ExpansionHubEx.CurrentDrawUnits.AMPS));
        telemetry.addData("Lift stage", currentLiftStage);
        telemetry.addData("Next Lift Stage", nextLiftStage);
        telemetry.addData("targetPos", targetPos);

        telemetry.update();
    }
}