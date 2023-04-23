import glob
import multiprocessing
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from functools import partial
from multiprocessing import Pool
from multiprocessing.pool import AsyncResult
from typing import Callable

import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
from numba import jit

from drive_simulation import DriveParameters, RobotState, RobotCommand, OptimisationParameters
from mecanum_data import DataSeries
from mecanum_data import T


def simulate(sample: DataSeries, robot: DriveParameters, graph_velocity=False, graph_position=False):
	start_pos = np.array([sample.x_position[0], sample.y_position[0], sample.angle[0]])
	start_vel = np.array([sample.x_velocity[0], sample.y_velocity[0], sample.angular_velocity[0]])

	robot_position = np.zeros((len(sample), 3))
	robot_velocity = np.zeros((len(sample), 3))
	robot_acceleration = np.zeros((len(sample), 3))
	robot_position[0] = start_pos
	robot_velocity[0] = start_vel

	for i in range(1, len(sample)):
		voltage = sample.battery_voltage[i - 1]
		powers = np.array([sample.fl[i - 1], sample.fr[i - 1], sample.bl[i - 1], sample.br[i - 1]])
		robot.battery_voltage = voltage
		robot_acceleration[i - 1] = robot.acceleration(RobotState(robot_position[i - 1], robot_velocity[i - 1], RobotCommand(powers)))
		robot_position[i] = robot_position[i - 1] + robot_velocity[i - 1] * T
		robot_velocity[i] = robot_velocity[i - 1] + robot_acceleration[i - 1] * T


	psi = robot_position[:, 2]
	rotation_matrices = np.zeros((len(sample), 3, 3))
	rotation_matrices[:, 0, 0] = np.cos(-psi)
	rotation_matrices[:, 0, 1] = -np.sin(-psi)
	rotation_matrices[:, 1, 0] = np.sin(-psi)
	rotation_matrices[:, 1, 1] = np.cos(-psi)
	rotation_matrices[:, 2, 2] = 1

	# multiply each row of the robot velocity by the rotation matrix
	robot_velocity = np.einsum('ijk,ik->ij', rotation_matrices, robot_velocity)

	if graph_velocity:
		plt.figure()
		plt.plot(sample.time, sample.x_velocity, label='X Velocity (Measured)')
		plt.plot(sample.time, sample.y_velocity, label='Y Velocity (Measured)')
		plt.plot(sample.time, sample.angular_velocity, label='Angular Velocity (Measured)')

		plt.plot(sample.time, robot_velocity[:, 0], label='X Velocity (Simulated)')
		plt.plot(sample.time, robot_velocity[:, 1], label='Y Velocity (Simulated)')
		plt.plot(sample.time, robot_velocity[:, 2], label='Angular Velocity (Simulated)')
		# plt.plot(sample.time, sample.angle, label='Angle (Measured)')
		# plt.plot(sample.time, robot_position[:, 2], label='Angle (Simulated)')

		plt.title(f"velocity {sample.name}")
		plt.legend()
		plt.show()

	if graph_position:
		plt.figure()
		plt.plot(sample.time, sample.angle, label='Angle (Measured)')
		plt.plot(sample.time, sample.x_position, label='X Position (Measured)')
		plt.plot(sample.time, sample.y_position, label='Y Position (Measured)')

		plt.plot(sample.time, robot_position[:, 2], label='Angle (Simulated)')
		plt.plot(sample.time, robot_position[:, 0], label='X Position (Simulated)')
		plt.plot(sample.time, robot_position[:, 1], label='Y Position (Simulated)')

		plt.title(f"position {sample.name}")
		plt.legend()
		plt.show()
	return np.sum(np.square(robot_velocity[:, 2] - sample.angular_velocity)) + np.sum(
		np.square(robot_velocity[:, 0] - sample.x_velocity)) + np.sum(
		np.square(robot_velocity[:, 1] - sample.y_velocity))


GRAD_STEP = .0001


def partial_derivative_jobs(objective, sample, args, parameter_name, pool: Pool) -> AsyncResult:
	args0 = args.copy()
	args1 = args.copy()

	args0[parameter_name] -= GRAD_STEP
	args1[parameter_name] += GRAD_STEP

	args_list = [(sample, DriveParameters.of_uniform_friction(**args0)), (sample, DriveParameters.of_uniform_friction(**args1))]

	def callback(result):
		return (result[1] - result[0]) / (2 * GRAD_STEP)

	# return promise and map
	return pool.starmap_async(objective, args_list, callback=callback)


def average_partial_derivative(samples, args, parameter_name, pool: Pool):
	jobs = [partial_derivative_jobs(simulate, sample, args, parameter_name, pool) for sample in samples]
	results = [job.get() for job in jobs]
	return np.mean(results, axis=0)


def grad(samples, args, param_names_grad, pool: Pool):
	grad = {}
	for param_name in param_names_grad:
		grad[param_name] = average_partial_derivative(samples, args, param_name, pool)
	return grad


if __name__ == '__main__':
	DO_MULTITHREADING = True  # this might kill your computer
	samples = [DataSeries.from_csv(f) for f in glob.glob('drive_samples/r2/*.csv')]
	args = {"motor_constant": 0.32789093279030673, "armature_resistance": 1.5257581040253128, "robot_mass": 13.35,
			"robot_moment": 1.702185068491449, "wheel_moment": 0.040191160528718295,
			"roller_moment": 0.0013551835768285448, "fl_wheel_friction": 0.4998943681626438,
			"fr_wheel_friction": 0.49997995361717945, "bl_wheel_friction": 0.4894342878586221,
			"br_wheel_friction": 0.4893501708472894, "fl_roller_friction": 0.228389900502004,
			"fr_roller_friction": 0.18235956537170453, "bl_roller_friction": 0.4608988724723372,
			"br_roller_friction": 0.2605485835073295}

	# print(simulate(samples[1], args, graph_velocity=True, graph_position=True))
	simulate(samples[0], DriveParameters(**args), graph_velocity=True, graph_position=True)
	# simulate(samples[2], args, graph_velocity=True, graph_position=True)

	with Pool(len(args) * 2 if DO_MULTITHREADING else 1) as p:
		for epoch_num in range(10000):
			g = grad(samples, args, args.keys(), p)
			args = {k: args[k] - g[k] * .0001 for k in args.keys()}

			print(f"epoch {epoch_num}, args: {repr(args)}")

	simulate(samples[0], args, graph_velocity=True, graph_position=True)
