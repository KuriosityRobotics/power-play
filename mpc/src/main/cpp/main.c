//
// Created by Alec Petridis on 1/30/23.
//

#include <stdlib.h>
#include "mecanum_mpc.h"
#include "matrix_functions.h"
#include <string.h>
#include <assert.h>
#include "interface.h"


#define NUMSTAGES 10
#define NUMVARS 10



void print_mecanum_mpc_info(mecanum_mpc_info* info) {
    printf("Solver info:\n");
    printf("  it            : %d\n", info->it);
    printf("  it2opt        : %d\n", info->it2opt);
    printf("  res_eq        : %.8f\n", info->res_eq);
    printf("  res_ineq      : %.8f\n", info->res_ineq);
    printf("  rsnorm        : %.8f\n", info->rsnorm);
    printf("  rcompnorm     : %.8f\n", info->rcompnorm);
    printf("  pobj          : %.8f\n", info->pobj);
    printf("  dobj          : %.8f\n", info->dobj);
    printf("  dgap          : %.8f\n", info->dgap);
    printf("  rdgap         : %.8f\n", info->rdgap);
    printf("  mu            : %.8f\n", info->mu);
    printf("  mu_aff        : %.8f\n", info->mu_aff);
    printf("  sigma         : %.8f\n", info->sigma);
    printf("  lsit_aff      : %d\n", info->lsit_aff);
    printf("  lsit_cc       : %d\n", info->lsit_cc);
    printf("  step_aff      : %.8f\n", info->step_aff);
    printf("  step_cc       : %.8f\n", info->step_cc);
    printf("  solvetime     : %.8f\n", info->solvetime);
    printf("  fevalstime    : %.8f\n", info->fevalstime);
}


void pretty_print_mpc_output(const mecanum_mpc_output* output) {
    const struct robot_state* x_ptr = (const struct robot_state*) output;
    const char* state_names[10] = {"x01", "x02", "x03", "x04", "x05",
                                   "x06", "x07", "x08", "x09", "x10"};
    for (int i = 0; i < 10; i++) {
        printf("%s:\n", state_names[i]);
        printf("  command: {%f, %f, %f, %f}\n",
               x_ptr[i].command[0], x_ptr[i].command[1],
               x_ptr[i].command[2], x_ptr[i].command[3]);
        printf("  position: {%f, %f, %f}\n",
               x_ptr[i].position[0], x_ptr[i].position[1], x_ptr[i].position[2]);
        printf("  velocity: {%f, %f, %f}\n",
               x_ptr[i].velocity[0], x_ptr[i].velocity[1], x_ptr[i].velocity[2]);
    }
}


int main() {
    mecanum_mpc_params *params = malloc(sizeof(mecanum_mpc_params));
    mecanum_mpc_output *output = malloc(sizeof(mecanum_mpc_output));
    mecanum_mpc_info *info = malloc(sizeof(mecanum_mpc_info));
    mecanum_mpc_mem *args = mecanum_mpc_internal_mem(0);
    FILE *debug_output = stdout;

    memset(params, 0, sizeof(mecanum_mpc_params));
    memset(output, 0, sizeof(mecanum_mpc_output));
    memset(info, 0, sizeof(mecanum_mpc_info));

    struct optimisation_parameters parameter_prototype = {
            .model = {
                    .motor_constant = 0.3,
                    .armature_resistance = 1.8,
                    .robot_mass = 13.35,
                    .robot_moment = 1.19,
                    .wheel_moment = 0.04,
                    .roller_moment = 0.002,
                    .fl_wheel_friction = 0.256,
                    .fr_wheel_friction = 0.256,
                    .bl_wheel_friction = 0.256,
                    .br_wheel_friction = 0.256,
                    .fl_roller_friction = 20.8,
                    .fr_roller_friction = 20.8,
                    .bl_roller_friction = 20.8,
                    .br_roller_friction = 20.8,
                    .battery_voltage = 12.0
            },
            .target = {
                    .position = {1, 0, 0},
                    .velocity = {0, 0, 0}
            },
            .weights = {
                    .motor_weights = {0., 0., 0., 0},
                    .robot_position_weights = {1, 1, 1},
                    .robot_velocity_weights = {0, 0, 0}
            }
    };

    for (struct optimisation_parameters* ptr = params->all_parameters; ptr < params->all_parameters + 10; ++ptr) {
        memcpy(ptr, &parameter_prototype, sizeof(parameter_prototype));
    }
    memset(&params->all_parameters[9].weights.motor_weights, 0, sizeof(double) * 4);

    mecanum_mpc_solve((mecanum_mpc_params *) params, output, info, args, debug_output, &mecanum_mpc_derivatives);

    print_mecanum_mpc_info(info);
    pretty_print_mpc_output(output);
}