package com.example.simplescheduleapp.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.ExceptionCode;
import org.junit.jupiter.api.function.Executable;

public abstract class CommonExceptionTest extends MockTestSupport {

    public void exceptionTest(Executable executable, ExceptionCode e) {
        ExceptionCode code = assertThrows(ApplicationException.class,
                executable
        ).getCode();
        assertThat(code).isEqualTo(e);
    }
}
