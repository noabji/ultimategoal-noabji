package org.firstinspires.ftc.teamcode;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.acmerobotics.roadrunner.trajectory.TrajectoryBuilder;
import com.acmerobotics.roadrunner.trajectory.constraints.DriveConstraints;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.drive.DriveConstants;
import org.firstinspires.ftc.teamcode.drive.mecanum.SampleMecanumDriveBase;
import org.firstinspires.ftc.teamcode.drive.mecanum.SampleMecanumDriveREV;

import com.disnodeteam.dogecv.detectors.skystone.SkystoneDetector;
import com.qualcomm.robotcore.hardware.TouchSensor;

import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;

@Autonomous(name = "REDAllAroundAuto", group = "Autonomous")
public class REDAllAroundAuto extends LinearOpMode {
    private OpenCvCamera phoneCam;
    private SkystoneDetector skyStoneDetector;

    private DcMotorEx intakeMotor1, intakeMotor2;
    private static DcMotorEx liftEx1;
    private Servo foundationServoLeft, foundationServoRight;

    // lift encoders
    static final double LIFT_COUNTS_PER_MOTOR_REV = 537.6;
    static final double LIFT_DRIVE_GEAR_REDUCTION = 1.0;
    static final double LIFT_WHEEL_DIAMETER_INCHES = 1.25;
    static final double LIFT_COUNTS_PER_INCH = (LIFT_COUNTS_PER_MOTOR_REV * LIFT_DRIVE_GEAR_REDUCTION) /
            (LIFT_WHEEL_DIAMETER_INCHES * 3.1415);  // 136.90275

    int currentLiftStage = 0;
    int targetPos = 0;
    double liftPower = 1;
    long liftUpTimer = -1;
    long intakeInTimer = -1;

    // Camera stuff
    String skystoneLoc = "";


    // hardware stuff
    private static Servo liftHoExt, wrist, grabber, stoneHolder;
    private TouchSensor liftTouch;
    private DistanceSensor intakeColor;

    // PIDF stuff
    PIDFCoefficients pidfCoefficients;
    double lkp = 6;
    double lki = 0;
    double lkd = 0;
    double lkf = 0;

    DriveConstraints stoneCollectionConstraints = new DriveConstraints(10.0, 10.0, 0.0, Math.toRadians(180.0), Math.toRadians(180.0), 0.0);


    public void runOpMode() {
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        phoneCam = new OpenCvInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);

        phoneCam.openCameraDevice();

        skyStoneDetector = new SkystoneDetector();
        phoneCam.setPipeline(skyStoneDetector);

        phoneCam.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);

        intakeMotor1 = hardwareMap.get(DcMotorEx.class, "intake motor 1");
        intakeMotor2 = hardwareMap.get(DcMotorEx.class, "intake motor 2");
        liftEx1 = hardwareMap.get(DcMotorEx.class, "lift motor 1");

        foundationServoLeft = hardwareMap.get(Servo.class, "foundationServoLeft");
        foundationServoRight = hardwareMap.get(Servo.class, "foundationServoRight");
        liftHoExt = hardwareMap.servo.get("liftHoExt");
        wrist = hardwareMap.servo.get("liftGrabberRotater");
        grabber = hardwareMap.servo.get("liftGrabber");
        stoneHolder = hardwareMap.servo.get("stoneHolder");

        intakeColor = hardwareMap.get(DistanceSensor.class, "intakeColor");
        liftTouch = hardwareMap.get(TouchSensor.class, "liftTouch");

        pidfCoefficients = new PIDFCoefficients(lkp, lki, lkd, lkf);

        intakeMotor1.setDirection(DcMotorSimple.Direction.REVERSE);
        liftEx1.setDirection(DcMotorSimple.Direction.REVERSE);
        liftEx1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Servo positions
        liftHoExt.setPosition(0.45);
        wrist.setPosition(0.18);
        grabber.setPosition(0.6);
        foundationServosUp();

        SampleMecanumDriveBase drive = new SampleMecanumDriveREV(hardwareMap);

        while (!opModeIsActive() && !isStopRequested()) {
            if (skyStoneDetector.isDetected()) {
                if (skyStoneDetector.getScreenPosition().x < 50) {
                    skystoneLoc = "left";
                } else if (skyStoneDetector.getScreenPosition().x < 190 && skyStoneDetector.getScreenPosition().x > 50) {
                    skystoneLoc = "center";
                } else {
                    skystoneLoc = "right";
                }
            }

            telemetry.addData("Skystone Location = " + skyStoneDetector.getScreenPosition().x, skystoneLoc);
            telemetry.addData("Status", "Waiting for start command...");
            telemetry.update();
        }

        if (opModeIsActive()) {
            moveForward(drive, 5);

            // FIRST SKYSTONE - OUTER ONE
            if (skystoneLoc.equals("right")) {
                strafeRight(drive, 20);
            } else if (skystoneLoc.equals("center")) {
                strafeRight(drive, 12);
            }

            moveForward(drive, 25);
            rotate(drive, 35);
            startIntakeMotors(-1);

            // moveForward(drive, 12);

            intakeInTimer = System.currentTimeMillis();
            while (System.currentTimeMillis() - intakeInTimer < 1000) { }
            stopIntakeMotors();

            /*stoneHolder.setPosition(0);
            sleep(200);
            stoneHolder.setPosition(0.4);
            sleep(200);
            stoneHolder.setPosition(0);
            grabber.setPosition(0.32);*/

            moveBackward(drive, 12);
            rotate(drive, 55);
            strafeLeft(drive, 12);
            moveBackward(drive, 70);

            rotate(drive, 90);
            startIntakeMotors(1);
            moveBackward(drive, 17);
            stopIntakeMotors();
            sleep(300);
            foundationServosDown();
            sleep(300);
            moveForward(drive, 20);

            // pulling the foundation part
            // rotate(drive, -90);
            while (drive.getExternalHeading() != -90) {
                drive.setMotorPowers(0.5, 0.5, -0.5, -0.5);
            }


            // moveBackward(drive, 7);

            // extendStone();
            // grabber.setPosition(0.6);
            // retractExt();

            foundationServosUp();
            moveForward(drive, 110);
            rotate(drive, -90);

            // ------------------------------------------------------------------------------------
            // LIFT DcMotorEx
            if (currentLiftStage == 0 && !liftTouch.isPressed()) {
                liftEx1.setTargetPosition(targetPos);
                liftEx1.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                liftEx1.setPower(-0.4);
            } else if (currentLiftStage == 0 && liftTouch.isPressed()) {
                liftEx1.setPower(0);
                liftEx1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            } else if (currentLiftStage != 0 && liftEx1.getCurrentPosition() != targetPos) {
                liftEx1.setTargetPosition(targetPos);
                liftEx1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                liftEx1.setPIDFCoefficients(DcMotor.RunMode.RUN_TO_POSITION, pidfCoefficients);
                liftEx1.setPower(liftPower);
            }
        }
    }

    private void extendStone() {
        currentLiftStage = 2;
        liftUpTimer = System.currentTimeMillis();

        while (System.currentTimeMillis() - liftUpTimer > 800) { }

        liftHoExt.setPosition(0.92);
        sleep(200);
        wrist.setPosition(0.18);
        currentLiftStage = 0;
    }

    private void retractExt() {
        wrist.setPosition(0.18);
        grabber.setPosition(0.32);
        sleep(200);
        liftHoExt.setPosition(0.45);
    }

    private void startIntakeMotors(double p) {
        intakeMotor1.setPower(p);
        intakeMotor2.setPower(p);
    }

    private void stopIntakeMotors() {
        intakeMotor1.setPower(0);
        intakeMotor2.setPower(0);
    }

    private void moveForward(SampleMecanumDriveBase thisDrive, double distance) {
        thisDrive.followTrajectorySync(thisDrive.trajectoryBuilder().forward(distance).build());
    }

    private void moveBackward(SampleMecanumDriveBase thisDrive, double distance) {
        thisDrive.followTrajectorySync(thisDrive.trajectoryBuilder().back(distance).build());
    }

    private void strafeLeft(SampleMecanumDriveBase thisDrive, double distance) {
        thisDrive.followTrajectorySync(thisDrive.trajectoryBuilder().strafeLeft(distance).build());
    }

    private void strafeRight(SampleMecanumDriveBase thisDrive, double distance) {
        thisDrive.followTrajectorySync(thisDrive.trajectoryBuilder().strafeRight(distance).build());
    }

    private void splineBotTo(SampleMecanumDriveBase thisDrive, double x, double y, double heading) {
        thisDrive.followTrajectorySync(thisDrive.trajectoryBuilder().splineTo(new Pose2d(x, y, heading)).build());
    }

    private void rotate(SampleMecanumDriveBase thisDrive, double angleInDeg) {
        thisDrive.turnSync(Math.toRadians(angleInDeg));
    }

    private void foundationServosDown() {
        foundationServoLeft.setPosition(0.21);
        foundationServoRight.setPosition(0.95);
    }

    private void foundationServosUp() {
        foundationServoLeft.setPosition(0.77);
        foundationServoRight.setPosition(0.37);
    }
}
