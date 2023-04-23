import glob

import matplotlib.pyplot as plt
import numpy as np

from drive_grad_descent_tune import simulate
from drive_simulation import DriveParameters
from mecanum_data import DataSeries

samples = [DataSeries.from_csv(f) for f in glob.glob('drive_samples/r2/*.csv')]

initial_guess = DriveParameters(
        motor_constant=.3,
        armature_resistance=1.8,
        robot_mass=13.35,
        robot_moment=1.19,
        wheel_moment=0.04,
        roller_moment=0.002,
        fl_wheel_friction=0.256,
        fr_wheel_friction=0.256,
        bl_wheel_friction=0.256,
        br_wheel_friction=0.256,
        fl_roller_friction=20.8,
        fr_roller_friction=20.8,
        bl_roller_friction=20.8,
        br_roller_friction=20.8,
)

for sample in samples:
	simulate(sample, initial_guess, graph_velocity=True, graph_position=False)


def objective(model: DriveParameters):
	return np.sum(np.square([simulate(s, model) for s in samples]))


NUM_SEARCH_TRIALS = 20000
MAX_CONCURRENT = 64

import optuna
import ray
from optuna.samplers import *
from optuna.distributions import FloatDistribution
from ray import tune
from ray.air import session
from ray.tune import TuneConfig
from ray.tune.search import ConcurrencyLimiter
from ray.tune.search.optuna import OptunaSearch

print("Optuna:", optuna.__version__, "Ray:", ray.__version__)
ray.init(log_to_driver=False, ignore_reinit_error=True)

sampler = NSGAIISampler()
search_alg = OptunaSearch(sampler=sampler, metric="loss", mode="min",
						  points_to_evaluate=[initial_guess.to_dict()])
search_alg = ConcurrencyLimiter(search_alg, max_concurrent=MAX_CONCURRENT)
tuner = tune.Tuner(
	lambda config: {"loss": objective(DriveParameters(**config))},
	tune_config=TuneConfig(
		search_alg=search_alg,
		mode="min",
		num_samples=NUM_SEARCH_TRIALS,
	),
	run_config=ray.air.RunConfig(verbose=1),
	param_space={
		"motor_constant": tune.uniform(0.30020907179123596, 0.30020907179123596),
		"armature_resistance": tune.uniform(1.7959487855263259, 1.7959487855263259),

		"robot_mass": tune.uniform(13.35, 13.35),
		"robot_moment": tune.uniform(0.9, 1.2),
		"wheel_moment": tune.uniform(0.04, 0.04),
		"roller_moment": tune.uniform(.002, .002),

		"fl_wheel_friction": tune.uniform(0., 0.5),
		"fr_wheel_friction": tune.uniform(0., 0.5),
		"bl_wheel_friction": tune.uniform(0., 0.5),
		"br_wheel_friction": tune.uniform(0., 0.5),

		"fl_roller_friction": tune.uniform(0., 50.),
		"fr_roller_friction": tune.uniform(0., 50.),
		"bl_roller_friction": tune.uniform(0., 50.),
		"br_roller_friction": tune.uniform(0., 50.),
	}
)

fit_results = tuner.fit()
best_result = DriveParameters(**fit_results.get_best_result('loss').config)
df = fit_results.get_dataframe()

plt.scatter(df.index[df['loss'] < 1e10], df['loss'][df['loss'] < 1e10])
plt.title("Objective vs Iteration")
plt.xlabel("Iteration")
plt.ylabel("Objective")
plt.show()

print(f"Best hyperparameters found were {best_result!r}")
for sample in samples:
	simulate(sample, best_result, graph_velocity=True, graph_position=False)

print(f"best loss is {np.min(df['loss'])}")
