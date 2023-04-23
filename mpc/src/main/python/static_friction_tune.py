from drive_simulation import DriveParameters
import numpy as np

static_friction = 0.5394

robot = DriveParameters(static_friction=np.array([[static_friction], [static_friction], [static_friction], [static_friction]]))
position = np.array([[0], [0], [0]])  # x, y, angle (meters, meters, radians)
velocity = np.array([[0], [0], [0]])  # units/second

powers = np.array([[0.06800000000000005], [0.06800000000000005], [0.06800000000000005], [0.06800000000000005]])  # powers must be between -1, 1 for each motor
voltage = 12.694  # volts

print("acceleration:")
print(robot.acceleration(position, velocity, powers, voltage))