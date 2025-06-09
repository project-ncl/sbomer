package org.jboss.sbomer.service.test.unit.feature.sbom;

import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.tekton.v1beta1.StepState;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunStatus;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.sbomer.service.feature.sbom.features.generator.rpm.controller.BrewRPMController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@QuarkusTest
public class BrewRPMControllerTest {


    private TaskRun dummyTaskRun(int exitcode) 
    {
        final String stepName = "test-step";
        TaskRun tr = Mockito.mock(TaskRun.class);
        TaskRunStatus trs = Mockito.mock(TaskRunStatus.class);
        StepState ss = Mockito.mock(StepState.class);
        ContainerStateTerminated cst = Mockito.mock(ContainerStateTerminated.class);

        when(tr.getStatus()).thenReturn(trs);
        when(trs.getSteps()).thenReturn(Collections.singletonList(ss));
        when(ss.getName()).thenReturn(stepName);
        when(ss.getTerminated()).thenReturn(cst);
        when(cst.getExitCode()).thenReturn(exitcode);
        return tr;
    }

    private TaskRun dummyTaskRunWithMultipleSteps(List<Integer> exitCodes) 
    {
        TaskRun tr = Mockito.mock(TaskRun.class);
        TaskRunStatus trs = Mockito.mock(TaskRunStatus.class);
        when(tr.getStatus()).thenReturn(trs);

        List<StepState> steps = new ArrayList<>();
        for (int i = 0; i < exitCodes.size(); i++) {
            StepState ss = Mockito.mock(StepState.class);
            ContainerStateTerminated cst = Mockito.mock(ContainerStateTerminated.class);

            when(ss.getName()).thenReturn("step-" + i);
            when(ss.getTerminated()).thenReturn(cst);
            when(cst.getExitCode()).thenReturn(exitCodes.get(i));
            steps.add(ss);
        }
        
        when(trs.getSteps()).thenReturn(steps);
        return tr;
}

    @Test
    void testGetFirstFailedStepExitCode()
    {
        assertEquals(10,  BrewRPMController.getFirstFailedStepExitCode(dummyTaskRun(10)));
        assertNotEquals(2,(int) BrewRPMController.getFirstFailedStepExitCode(dummyTaskRun(10)));
        assertNull(BrewRPMController.getFirstFailedStepExitCode(dummyTaskRun(0)));

        assertEquals(10, BrewRPMController.getFirstFailedStepExitCode(dummyTaskRunWithMultipleSteps(Arrays.asList(0, 0, 10))));
        assertEquals(5, BrewRPMController.getFirstFailedStepExitCode(dummyTaskRunWithMultipleSteps(Arrays.asList(0, 5, 10))));
        assertNull(BrewRPMController.getFirstFailedStepExitCode(dummyTaskRunWithMultipleSteps(Arrays.asList(0, 0, 0))));
    }
}