#include "model.h"
#include "mecanum_mpc.h"
#include "matrix_functions.h"
#include <jni.h>
#include <stdlib.h>
#include <assert.h>
#include <stdbool.h>


jobject construct_jsolution_info(JNIEnv *env, const mecanum_mpc_info *info);

jobject construct_jsolver_output(JNIEnv *env, int exitCode, jobject solutionInfo, jobjectArray systemStates);

jobjectArray construct_jsystem_state_array(JNIEnv *env, const mecanum_mpc_output *output);

static void mecanum_mpc_sparse2fullcopy(solver_int32_default nrow, solver_int32_default ncol, const solver_int32_default *colidx, const solver_int32_default *row, mecanum_mpc_callback_float *data, mecanum_mpc_float *out)
{
    solver_int32_default i, j;

    /* copy data into dense matrix */
    for(i=0; i<ncol; i++)
    {
        for(j=colidx[i]; j<colidx[i+1]; j++)
        {
            out[i*nrow + row[j]] = ((mecanum_mpc_float) data[j]);
        }
    }
}




extern solver_int32_default mecanum_mpc_derivatives(mecanum_mpc_float *x,        /* primal vars                                         */
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
								 solver_int32_default threadID   /* Id of caller thread                                */)
{
    /* AD tool input and output arrays */
    const mecanum_mpc_callback_float *in[4];
    mecanum_mpc_callback_float *out[7];


	/* Allocate working arrays for AD tool */
	mecanum_mpc_float w[573];

    /* temporary storage for AD tool sparse output */
    mecanum_mpc_callback_float this_f = (mecanum_mpc_callback_float) 0.0;
    mecanum_mpc_float nabla_f_sparse[10];


    mecanum_mpc_float c_sparse[6];
    mecanum_mpc_float nabla_c_sparse[50];


    /* pointers to row and column info for
     * column compressed format used by AD tool */
    solver_int32_default nrow, ncol;
    const solver_int32_default *colind, *row;

    /* set inputs for AD tool */
    in[0] = x;
    in[1] = p;
    in[2] = l;
    in[3] = y;

	if ((0 <= stage && stage <= 8))
	{


		out[0] = &this_f;
		out[1] = nabla_f_sparse;
		mecanum_mpc_objective_0(in, out, NULL, w, 0);
		if( nabla_f )
		{
			nrow = mecanum_mpc_objective_0_sparsity_out(1)[0];
			ncol = mecanum_mpc_objective_0_sparsity_out(1)[1];
			colind = mecanum_mpc_objective_0_sparsity_out(1) + 2;
			row = mecanum_mpc_objective_0_sparsity_out(1) + 2 + (ncol + 1);
			mecanum_mpc_sparse2fullcopy(nrow, ncol, colind, row, nabla_f_sparse, nabla_f);
		}

		out[0] = c_sparse;
		out[1] = nabla_c_sparse;
		mecanum_mpc_dynamics_0(in, out, NULL, w, 0);
		if( c )
		{
			nrow = mecanum_mpc_dynamics_0_sparsity_out(0)[0];
			ncol = mecanum_mpc_dynamics_0_sparsity_out(0)[1];
			colind = mecanum_mpc_dynamics_0_sparsity_out(0) + 2;
			row = mecanum_mpc_dynamics_0_sparsity_out(0) + 2 + (ncol + 1);
			mecanum_mpc_sparse2fullcopy(nrow, ncol, colind, row, c_sparse, c);
		}
		if( nabla_c )
		{
			nrow = mecanum_mpc_dynamics_0_sparsity_out(1)[0];
			ncol = mecanum_mpc_dynamics_0_sparsity_out(1)[1];
			colind = mecanum_mpc_dynamics_0_sparsity_out(1) + 2;
			row = mecanum_mpc_dynamics_0_sparsity_out(1) + 2 + (ncol + 1);
			mecanum_mpc_sparse2fullcopy(nrow, ncol, colind, row, nabla_c_sparse, nabla_c);
		}
	}
	if ((9 == stage))
	{


		out[0] = &this_f;
		out[1] = nabla_f_sparse;
		mecanum_mpc_objective_1(in, out, NULL, w, 0);
		if( nabla_f )
		{
			nrow = mecanum_mpc_objective_1_sparsity_out(1)[0];
			ncol = mecanum_mpc_objective_1_sparsity_out(1)[1];
			colind = mecanum_mpc_objective_1_sparsity_out(1) + 2;
			row = mecanum_mpc_objective_1_sparsity_out(1) + 2 + (ncol + 1);
			mecanum_mpc_sparse2fullcopy(nrow, ncol, colind, row, nabla_f_sparse, nabla_f);
		}
	}

    /* add to objective */
    if (f != NULL)
    {
        *f += ((mecanum_mpc_float) this_f);
    }

    return 0;
}

JNIEXPORT jobject construct_jsolution_info(JNIEnv *env, const mecanum_mpc_info *info) {
    jclass solutionInfoClass = (*env)->FindClass(env, "com/kuriosityrobotics/powerplay/mpc/SolutionInfo");
    jobject solutionInfo = (*env)->AllocObject(env, solutionInfoClass);

    (*env)->SetIntField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "it", "I"), info->it);
    (*env)->SetIntField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "it2opt", "I"), info->it2opt);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "resEq", "D"), info->res_eq);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "resIneq", "D"),
                           info->res_ineq);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "rsnorm", "D"), info->rsnorm);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "rcompnorm", "D"),
                           info->rcompnorm);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "pobj", "D"), info->pobj);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "dobj", "D"), info->dobj);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "dgap", "D"), info->dgap);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "rdgap", "D"), info->rdgap);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "mu", "D"), info->mu);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "muAff", "D"), info->mu_aff);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "sigma", "D"), info->sigma);
    (*env)->SetIntField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "lsitAff", "I"),
                           info->lsit_aff);
    (*env)->SetIntField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "lsitCc", "I"),
                           info->lsit_cc);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "stepAff", "D"),
                           info->step_aff);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "stepCc", "D"),
                           info->step_cc);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "solvetime", "D"),
                           info->solvetime);
    (*env)->SetDoubleField(env, solutionInfo, (*env)->GetFieldID(env, solutionInfoClass, "fevalstime", "D"),
                           info->fevalstime);
    return solutionInfo;
}

JNIEXPORT jobject JNICALL Java_com_kuriosityrobotics_powerplay_mpc_SolverInput_solve(
        JNIEnv *env,
        jobject obj
) {
    mecanum_mpc_output *output = malloc(sizeof(mecanum_mpc_output));
    mecanum_mpc_info *info = malloc(sizeof(mecanum_mpc_info));
    mecanum_mpc_mem *args = mecanum_mpc_internal_mem(0);
    FILE *debug_output = stdout;

    memset(output, 0, sizeof(mecanum_mpc_output));
    memset(info, 0, sizeof(mecanum_mpc_info));

    jclass solverInputClass = (*env)->GetObjectClass(env, obj);
    jmethodID getDoubleArray = (*env)->GetMethodID(env, solverInputClass, "toDoubleArray", "()[D");

    jdoubleArray _params = (*env)->CallObjectMethod(env,
                                                    obj,
                                                    getDoubleArray);
    mecanum_mpc_params *params = (mecanum_mpc_params *) (*env)->GetDoubleArrayElements(env,
                                                                                       _params,
                                                                                       NULL);

    int exitCode = mecanum_mpc_solve((mecanum_mpc_params *) params, output, info, args, debug_output, mecanum_mpc_derivatives);
    jobject solutionInfo = construct_jsolution_info(env, info);
    jobjectArray systemStates = construct_jsystem_state_array(env, output);

    jobject solverOutput = construct_jsolver_output(env, exitCode, solutionInfo, systemStates);

    (*env)->ReleaseDoubleArrayElements(env, _params, (jdouble *) params, 0);
    free(output);
    free(info);
    return solverOutput;
}

jobjectArray construct_jsystem_state_array(JNIEnv *env, const mecanum_mpc_output *output) {
    jclass systemStateClass = (*env)->FindClass(env, "com/kuriosityrobotics/powerplay/mpc/SystemState");
    jmethodID systemStateFromDoubleArray = (*env)->GetStaticMethodID(env, systemStateClass, "from", "([D)Lcom/kuriosityrobotics/powerplay/mpc/SystemState;");
    int num_stages =  sizeof (*output) / sizeof (output->x01);
    jobjectArray systemStates = (*env)->NewObjectArray(env, num_stages, systemStateClass, NULL);
    for (int i = 0; i < sizeof (*output) / sizeof (output->x01); i++) {
        jdoubleArray systemStateArray = (*env)->NewDoubleArray(env, 10);
        (*env)->SetDoubleArrayRegion(env, systemStateArray, 0, 10, (jdouble *) (&(&output->x01)[i]));
        (*env)->SetObjectArrayElement(env, systemStates, i, (*env)->CallStaticObjectMethod(env, systemStateClass, systemStateFromDoubleArray, systemStateArray));
    }
    return systemStates;
}

jobject construct_jsolver_output(JNIEnv *env, int exitCode, jobject solutionInfo, jobjectArray systemStates) {
    jclass solverOutputClass = (*env)->FindClass(env, "com/kuriosityrobotics/powerplay/mpc/SolverOutput");

    jobject solverOutput = (*env)->AllocObject(env, solverOutputClass);
    (*env)->SetObjectField(env, solverOutput, (*env)->GetFieldID(env, solverOutputClass, "states", "[Lcom/kuriosityrobotics/powerplay/mpc/SystemState;"), systemStates);
    (*env)->SetObjectField(env, solverOutput, (*env)->GetFieldID(env, solverOutputClass, "solutionInfo", "Lcom/kuriosityrobotics/powerplay/mpc/SolutionInfo;"), solutionInfo);
    (*env)->SetIntField(env, solverOutput, (*env)->GetFieldID(env, solverOutputClass, "exitCode", "I"), exitCode);
    return solverOutput;
}
