import numpy as np
import gymnasium as gym
from gymnasium import spaces
from gymnasium.wrappers import FlattenObservation
from drive_simulation import DriveModel

BATTERY_VOLTAGE = 12.
TIMESTEP = 0.1
MODEL = DriveModel(
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


class DriveTrainEnv(gym.Env):
    metadata = {"render_modes": ["human", "rgb_array"], "render_fps": 4}
    def __init__(self, render_mode=None, size=10., max_speed=3., max_angular_speed=15):
        super().__init__()
        self.min_state = np.array([0,0,-2*np.pi,-max_speed,-max_speed,-max_angular_speed])
        self.max_state = np.array([size,size,2*np.pi,max_speed,max_speed,max_angular_speed])
        
        self.min_target = np.array([0,0,-2*np.pi])
        self.max_target = np.array([size, size, 2*np.pi])

        self.max_angular_speed = max_angular_speed
        self.window_size = 512  # The size of the PyGame window

        # Observations are dictionaries with the agent's and the target's location.
        # Each location is encoded as an element of {0, ..., `size`}^2, i.e. MultiDiscrete([size, size]).
        self.observation_space = spaces.Dict(
            {
                # the possible range of positions are infinite, but the possible starting positions are limited
                "agent": spaces.Box(low=-np.Inf, high=np.Inf, shape=(6,), dtype=float), # pos and vel
                "target": spaces.Box(low=self.min_target, high=self.max_target, dtype=float),# pos only (vel target is always 0)
            }
        )

        # We have 4 different motors to control
        self.action_space = spaces.Box(low=-1., high=1., shape=(4,), dtype=float)

        assert render_mode is None or render_mode in self.metadata["render_modes"]
        self.render_mode = render_mode

        """
        If human-rendering is used, `self.window` will be a reference
        to the window that we draw to. `self.clock` will be a clock that is used
        to ensure that the environment is rendered at the correct framerate in
        human-mode. They will remain `None` until human-mode is used for the
        first time.
        """
        self.window = None
        self.clock = None

        def _get_obs(self):
            return np.concatenate([self._agent_location, self._target_location], axis=1)# {"agent": self._agent_location, "target": self._target_location}
        
        def _get_info(self):
            return {}# {"distance": np.linalg.norm(self._agent_location - self._target_location, ord=1)}
        
        def reset(self, seed=None, options=None):
            # We need the following line to seed self.np_random
            super().reset(seed=seed)

            # Choose the agent's location uniformly at random
            self._agent_location = self.np_random.integers(low=self.min_state, size=self.max_state, dtype=float)

            # We will sample the target's location randomly until it does not coincide with the agent's location
            self._target_location = self._agent_location[:3]
            while np.array_equal(self._target_location, self._agent_location[:3]):
                self._target_location = self.np_random.integers(
                    low=self.min_target, high=self.max_target, dtype=float
                )

            observation = self._get_obs()
            info = self._get_info()

            if self.render_mode == "human":
                self._render_frame()

            return observation, info
        
        def step(self, action):        
            # action is a 1x4 array containing [frontleft_motor_power, frontright_motor_power, backleft_motor_power, backright_motor_power]
            # action must be between [-1,1]
            action = np.clip(action, -1, 1)

            position = self._agent_location[:3].copy()
            velocity = self._agent_location[3:6].copy()
            acceleration = MODEL.acceleration(position, velocity, inputs, BATTERY_VOLTAGE)
            position = position + velocity * TIMESTEP # standard euler forwards integration
            velocity = velocity + acceleration * TIMESTEP 
            self._agent_location = np.concatenate([position, velocity], axis=1)
            
            # An episode is done if the agent has reached the target
            # reward is negative of cost
            reward = -(20*np.sum(np.square(position-_target_location)) + 10*np.sum(np.square(velocity)) + 1*np.sum(np.square(action)))
            
            terminated = (-reward < 0.01)

            observation = self._get_obs()
            info = self._get_info()

            if self.render_mode == "human":
                self._render_frame()

            return observation, reward, terminated, False, info