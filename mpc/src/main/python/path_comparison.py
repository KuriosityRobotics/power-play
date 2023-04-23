import pandas as pd
import numpy as np
from mpc.src.main.python.mecanum_data import DataSeries
from mpc.src.main.python.mecanum_mpc.path_data import PathData
import matplotlib.pyplot as plt

inputPath = PathData.from_csv("../resources/path.csv")
driveLog = DataSeries.from_csv("drive_samples/mpc_log_5.csv")

fig, ax = plt.subplots()
inputPath.plot(ax)
driveLog.plot(ax)
plt.show()
