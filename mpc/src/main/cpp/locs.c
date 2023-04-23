#include <stdint.h>
#include "mecanum_mpc.h"
uint8_t* mecanum_mpc_mem_internal = 0LL;

void * mecanum_mpc_internal_mem(int a1)
{
  if ( a1 )
    return 0LL;
  if ( !mecanum_mpc_mem_internal ){
    mecanum_mpc_mem_internal = (uint8_t*)malloc(mecanum_mpc_get_mem_size());
    mecanum_mpc_init_mem(mecanum_mpc_mem_internal, 0LL);
    }
  return mecanum_mpc_mem_internal;
}