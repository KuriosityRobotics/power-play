import matplotlib.pyplot as plt
import numpy as np
np.set_printoptions(suppress=True)

import pandas as pd

from mecanum_data import DataSeries

if __name__ == '__main__':
	sample = DataSeries.from_csv('drive_samples/r2/spin.csv')
	sample = sample[15:]
	rotation_matrices = np.zeros((len(sample), 2, 2))
	rotation_matrices[:, 0, 0] = np.cos(sample.angle)
	rotation_matrices[:, 0, 1] = -np.sin(sample.angle)
	rotation_matrices[:, 1, 0] = np.sin(sample.angle)
	rotation_matrices[:, 1, 1] = np.cos(sample.angle)

	lhs = np.linalg.inv(rotation_matrices - np.eye(2))
	geometric_centre_positions = np.vstack((sample.x_velocity, sample.y_velocity)).T
	calculated_com_offsets = np.einsum('bcd, bc -> bc', lhs, geometric_centre_positions)

	df = pd.DataFrame(calculated_com_offsets, columns=['x', 'y'])
	fig, ax = plt.subplots()
	# set limits
	df.plot(kind = 'hist', bins = 100, ax = ax, density = True)
	df.plot(kind = 'kde', ax = ax)
	plt.show()

	calculated_com_locations = geometric_centre_positions - np.einsum('bcd, bc -> bc', rotation_matrices, calculated_com_offsets)
	print(geometric_centre_positions, calculated_com_offsets, calculated_com_locations, sep='\n')
	fig, ax = plt.subplots()
	ax.quiver(sample.x_position, sample.y_position, calculated_com_locations[:, 0], calculated_com_locations[:, 1], scale=1, width=0.0025, headwidth=3, headlength=4, headaxislength=3.5)
	# ax.set_xlim(-1, 1)
	# ax.set_ylim(-1, 1)
	plt.show()


