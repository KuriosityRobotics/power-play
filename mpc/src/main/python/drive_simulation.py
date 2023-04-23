import dataclasses
from dataclasses import dataclass
from typing import Iterable

import casadi
from numba import njit

if 'disabled' in njit.__dict__:
	import aerosandbox.numpy as np
else:
	import numpy as np

static_friction_threshold = 0.2


@njit
def rotation_matrix(psi):
	return np.array([[np.cos(psi), -np.sin(psi), 0.], [np.sin(psi), np.cos(psi), 0.], [0., 0., 1.]])


@njit
def rotation_matrix_derivative(psi, psidot):
	return np.array([[-np.sin(psi), -np.cos(psi), 0.], [np.cos(psi), -np.sin(psi), 0.], [0., 0., 0.]]) * psidot


def angular_velocity_R(roller_angle, wheel_radius, s, d):
	A = np.sin(roller_angle) / (wheel_radius * np.sin(roller_angle))
	B = -np.cos(roller_angle) / (wheel_radius * np.sin(roller_angle))
	C = (-d * np.sin(roller_angle) - s * np.cos(roller_angle)) / (wheel_radius * np.sin(roller_angle))
	return np.stack([A, B, C], axis=-1)


def contact_point_velocity_R(roller_angle, s, d):
	a = np.zeros_like(roller_angle)
	b = 1 / np.sin(roller_angle)
	c = s / np.sin(roller_angle)
	return np.stack([a, b, c], axis=-1)
@dataclass
class RobotCommand:
	powers: np.ndarray

	@property
	def fl(self):
		return self.powers[0]

	@property
	def fr(self):
		return self.powers[1]

	@property
	def bl(self):
		return self.powers[2]

	@property
	def br(self):
		return self.powers[3]

	def to_array(self):
		return np.array([self.fl, self.fr, self.bl, self.br])

	@staticmethod
	def from_array(array):
		return RobotCommand(array)

@dataclass
class RobotState:
	command: RobotCommand
	position: np.ndarray
	velocity: np.ndarray

	@property
	def x(self):
		return self.position[0]

	@property
	def y(self):
		return self.position[1]

	@property
	def angle(self):
		return self.position[2]

	@property
	def vx(self):
		return self.velocity[0]

	@property
	def vy(self):
		return self.velocity[1]

	@property
	def vangle(self):
		return self.velocity[2]

	def to_array(self):
		return np.concatenate((self.command.to_array(), self.position, self.velocity))

	@staticmethod
	def from_array(array):
		return RobotState(RobotCommand.from_array(array[:4]), array[4:7], array[7:])


@dataclass
class RobotStateTarget:
	position: np.ndarray
	velocity: np.ndarray

	@property
	def x(self):
		return self.position[0]

	@property
	def y(self):
		return self.position[1]

	@property
	def angle(self):
		return self.position[2]

	@property
	def vx(self):
		return self.velocity[0]

	@property
	def vy(self):
		return self.velocity[1]

	@property
	def vangle(self):
		return self.velocity[2]

	def to_array(self):
		return np.array([self.x, self.y, self.angle, self.vx, self.vy, self.vangle])

	@staticmethod
	def from_array(array):
		return RobotStateTarget(array[:3], array[3:])

	@staticmethod
	def num_parameters():
		return 6


ROBOT_FORWARDS_AXIS: float = 0.115
ROBOT_SIDEWAYS_AXIS: float = 0.1325
WHEEL_RADIUS: float = 0.048

d = np.array([1, -1, 1, -1]) * ROBOT_SIDEWAYS_AXIS
s = np.array([1, 1, -1, -1]) * ROBOT_FORWARDS_AXIS
roller_angles = np.array([np.pi / 4, -np.pi / 4, -np.pi / 4, np.pi / 4])

angular_velocity_rows = angular_velocity_R(roller_angles, WHEEL_RADIUS, s, d)
contact_point_velocity_rows = contact_point_velocity_R(roller_angles, s, d)
R = np.vstack((angular_velocity_rows, contact_point_velocity_rows))

@njit
def _applied_torque(wheel_velocity, powers, voltage, motor_constant, armature_resistance):
	ea = voltage * powers
	eb = wheel_velocity * motor_constant
	return (ea - eb) / armature_resistance


# @njit
def _net_torque(wheel_roller_velocity, powers, voltage, motor_constant, armature_resistance, dynamic_friction):
	wheel_velocity = wheel_roller_velocity[:4]

	applied_torque_wheel = _applied_torque(wheel_velocity, powers, voltage, motor_constant, armature_resistance)
	applied_torque = np.concatenate((applied_torque_wheel, np.zeros(4)))

	net_torque = applied_torque - (2/(1 + np.exp(-10 *wheel_roller_velocity)) - 1) * dynamic_friction
	# use smooth sigmoid instead of np.sign
	return net_torque


# @njit
def _acceleration(position, velocity, powers, voltage, motor_constant, armature_resistance, dynamic_friction, M_r, M_w):
	angle = position[2]
	angular_velocity = velocity[2]

	rotation = rotation_matrix(angle)
	rotation_dot = rotation_matrix_derivative(angle, angular_velocity)

	wheel_velocity = R @ rotation.T @ velocity

	H = M_r + rotation @ R.T @ M_w @ R @ np.linalg.inv(rotation)
	K = rotation @ R.T @ M_w @ R @ rotation_dot.T
	F_a = rotation @ R.T @ _net_torque(wheel_velocity, powers, voltage, motor_constant, armature_resistance,
									   dynamic_friction)
	acceleration = np.linalg.inv(H) @ (F_a - K @ velocity)
	return acceleration


@dataclass
class DriveParameters:
	motor_constant: float = .3
	armature_resistance: float = 1.8

	robot_mass: float = 13.35
	robot_moment: float = 1.19
	wheel_moment: float = 0.04
	roller_moment: float = 0.002

	fl_wheel_friction: float = 0.256
	fr_wheel_friction: float = 0.256
	bl_wheel_friction: float = 0.256
	br_wheel_friction: float = 0.256

	fl_roller_friction: float = 20.8
	fr_roller_friction: float = 20.8
	bl_roller_friction: float = 20.8
	br_roller_friction: float = 20.8

	battery_voltage: float = 12

	@staticmethod
	def of_uniform_friction(**kwargs):
		if 'roller_friction' in kwargs:
			friction = kwargs['roller_friction']
			kwargs['fl_roller_friction'] = friction
			kwargs['fr_roller_friction'] = friction
			kwargs['bl_roller_friction'] = friction
			kwargs['br_roller_friction'] = friction
			del kwargs['roller_friction']

		if 'wheel_friction' in kwargs:
			friction = kwargs['wheel_friction']
			kwargs['fl_wheel_friction'] = friction
			kwargs['fr_wheel_friction'] = friction
			kwargs['bl_wheel_friction'] = friction
			kwargs['br_wheel_friction'] = friction
			del kwargs['wheel_friction']

		return DriveParameters(**kwargs)


	def to_dict(self):
		result = dataclasses.asdict(self)
		del result['battery_voltage']


	def to_uniform_friction_dict(self):
		"""
		Converts the drive model to a dictionary with uniform wheel friction
		"""
		if not self.is_uniform_friction():
			raise ValueError('DriveModel does not have uniform wheel friction')

		result = dataclasses.asdict(self)
		result['roller_friction'] = result['fl_roller_friction']
		del result['fl_roller_friction']
		del result['fr_roller_friction']
		del result['bl_roller_friction']
		del result['br_roller_friction']

		result['wheel_friction'] = result['fl_wheel_friction']
		del result['fl_wheel_friction']
		del result['fr_wheel_friction']
		del result['bl_wheel_friction']
		del result['br_wheel_friction']

		del result['battery_voltage']

		return result

	def to_array(self):
		return np.array([
			self.motor_constant,
			self.armature_resistance,
			self.robot_mass,
			self.robot_moment,
			self.wheel_moment,
			self.roller_moment,
			self.fl_wheel_friction,
			self.fr_wheel_friction,
			self.bl_wheel_friction,
			self.br_wheel_friction,
			self.fl_roller_friction,
			self.fr_roller_friction,
			self.bl_roller_friction,
			self.br_roller_friction,
			self.battery_voltage
		])

	@staticmethod
	def from_array(array):
		return DriveParameters(
			motor_constant=array[0],
			armature_resistance=array[1],
			robot_mass=array[2],
			robot_moment=array[3],
			wheel_moment=array[4],
			roller_moment=array[5],
			fl_wheel_friction=array[6],
			fr_wheel_friction=array[7],
			bl_wheel_friction=array[8],
			br_wheel_friction=array[9],
			fl_roller_friction=array[10],
			fr_roller_friction=array[11],
			bl_roller_friction=array[12],
			br_roller_friction=array[13],
			battery_voltage=array[14]
		)

	def is_uniform_friction(self):
		"""
		Checks if the drive model has uniform wheel and roller friction
		:return: True if the drive model has uniform wheel and roller friction, False otherwise
		"""
		return (
			self.fl_roller_friction == self.fr_roller_friction == self.bl_roller_friction == self.br_roller_friction and
			self.fl_wheel_friction == self.fr_wheel_friction == self.bl_wheel_friction == self.br_wheel_friction
		)

	def __post_init__(self):
		self.M_r = np.diag(np.array([self.robot_mass, self.robot_mass, self.robot_moment]))
		self.M_w = np.diag(np.concatenate((
			np.ones(4) * self.wheel_moment,
			np.ones(4) * self.roller_moment
		)))

		self.dynamic_friction = np.array([
			self.fl_wheel_friction,
			self.fr_wheel_friction,
			self.bl_wheel_friction,
			self.br_wheel_friction,

			self.fl_roller_friction,
			self.fr_roller_friction,
			self.bl_roller_friction,
			self.br_roller_friction
		])

	def acceleration(self, state: RobotState):
		return _acceleration(
			state.position,
			state.velocity,
			state.command.powers,
			self.battery_voltage,
			self.motor_constant,
			self.armature_resistance,
			self.dynamic_friction,
			self.M_r,
			self.M_w
		)

	def continuous_dynamics(self, state: RobotState):
		velocity = state.velocity
		acceleration = self.acceleration(state)
		return np.concatenate((velocity, acceleration))

	def __repr__(self):
		"""
		prettier repr
		:return:
		"""
		result = f"{self.__class__.__name__}(\n"
		for field in dataclasses.fields(self):
			result += f"\t{field.name}={getattr(self, field.name)},\n"

		result += ")"
		return result

	@staticmethod
	def num_parameters():
		return 15


@dataclass
class ObjectiveWeights:
	motor_weights: np.ndarray = np.array([0., 0., 0., 0.])
	robot_position_weights: np.ndarray = np.array([0., 0., 0.])
	robot_velocity_weights: np.ndarray = np.array([0., 0., 0.])

	def to_array(self):
		return np.concatenate((
			self.motor_weights,
			self.robot_position_weights,
			self.robot_velocity_weights
		))

	@staticmethod
	def from_array(array):
		return ObjectiveWeights(
			motor_weights=array[:4],
			robot_position_weights=array[4:7],
			robot_velocity_weights=array[7:]
		)

	@staticmethod
	def num_parameters():
		return 10


@dataclass
class OptimisationParameters:
	model: DriveParameters
	target: RobotStateTarget
	weights: ObjectiveWeights

	def to_array(self):
		return np.concatenate((
			self.model.to_array(),
			self.target.to_array(),
			self.weights.to_array()
		))

	@staticmethod
	def from_array(array):
		num_drive = DriveParameters.num_parameters()
		num_target = RobotStateTarget.num_parameters()
		return OptimisationParameters(
			model=DriveParameters.from_array(array[:num_drive]),
			target=RobotStateTarget.from_array(array[num_drive:num_drive + num_target]),
			weights=ObjectiveWeights.from_array(array[num_drive + num_target:])
		)

	@staticmethod
	def num_parameters():
		return DriveParameters.num_parameters() + RobotStateTarget.num_parameters() + ObjectiveWeights.num_parameters()

	def objective(self, state: RobotState):
		"""
		Computes the objective function for the optimisation
		:param state: The current state of the robot
		:param command: The current command to the robot
		:return: The objective value
		"""
		motor_weights = self.weights.motor_weights
		robot_position_weights = self.weights.robot_position_weights
		robot_velocity_weights = self.weights.robot_velocity_weights

		motor_objective = np.sum(motor_weights * state.command.powers**2)
		robot_position_objective = np.sum(robot_position_weights * (self.target.position - state.position)**2)
		robot_velocity_objective = np.sum(robot_velocity_weights * (self.target.velocity - state.velocity)**2)

		return motor_objective + robot_position_objective + robot_velocity_objective






if __name__ == '__main__':
	robot = DriveParameters()
	print(repr(robot))
	position = np.array([0., 0., 0.])  # x, y, angle (meters, meters, radians)
	velocity = np.array([0., 0., 0.])  # units/second

	powers = np.array([1., 1., 1., 1.])  # powers must be between -1, 1 for each motor
	voltage = 12  # volts

	print("acceleration:")
	print(robot.acceleration(position, velocity, powers, voltage))

	velocity_forwards = np.array([1, 0, 0])
	velocity_sideways = np.array([0, 1, 0])

	print("\n\n")

	print("forwards: ")
	print(R @ velocity_forwards)

	print("\n")

	print("sideways: ")
	print(R @ velocity_sideways)

