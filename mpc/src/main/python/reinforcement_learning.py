import numpy as np
import gymnasium as gym
from drivetrain_gym_environment import DriveTrainEnv

env = DriveTrainEnv()
from stable_baselines3 import SAC
policy = SAC("MlpPolicy", env, verbose=1)

policy.learn(10000)

# evaluation code
vec_env = policy.get_env()
obs = vec_env.reset()
for i in range(1000):
    action, _state = model.predict(obs, deterministic=True)
    obs, reward, done, info = vec_env.step(action)
    vec_env.render()
    # VecEnv resets automatically
    # if done:
    #   obs = vec_env.reset()