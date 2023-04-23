//
// Created by Alec Petridis on 2/3/23.
//

#ifndef LIGMA_INTERFACE_H
#define LIGMA_INTERFACE_H

#include <jni.h>
#include "mecanum_mpc.h"

solver_int32_default mecanum_mpc_derivatives(mecanum_mpc_float *x,        /* primal vars                                         */
                                               mecanum_mpc_float *y,        /* eq. constraint multiplers                           */
                                               mecanum_mpc_float *l,        /* ineq. constraint multipliers                        */
                                               mecanum_mpc_float *p,        /* parameters                                          */
                                               mecanum_mpc_float *f,        /* objective function (scalar)                         */
                                               mecanum_mpc_float *nabla_f,  /* gradient of objective function                      */
                                               mecanum_mpc_float *c,        /* dynamics                                            */
                                               mecanum_mpc_float *nabla_c,  /* Jacobian of the dynamics (column major)             */
                                               mecanum_mpc_float *h,        /* inequality constraints                              */
                                               mecanum_mpc_float *nabla_h,  /* Jacobian of inequality constraints (column major)   */
                                               mecanum_mpc_float *hess,     /* Hessian (column major)                              */
                                               solver_int32_default stage,     /* stage number (0 indexed)                           */
                                               solver_int32_default iteration, /* iteration number of solver                         */
                                               solver_int32_default threadID   /* Id of caller thread                                */);


JNIEXPORT jobjectArray JNICALL Java_com_kuriosityrobotics_powerplay_mpc_SolverInput_solve(
	JNIEnv *env,
	jobject obj
);
#endif
