################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../jni/DetectionBasedTracker_jni.cpp 

OBJS += \
./jni/DetectionBasedTracker_jni.o 

CPP_DEPS += \
./jni/DetectionBasedTracker_jni.d 


# Each subdirectory must supply rules for building sources it contributes
jni/DetectionBasedTracker_jni.o: ../jni/DetectionBasedTracker_jni.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C++ Compiler'
	g++ -O2 -g -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"jni/DetectionBasedTracker_jni.d" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


