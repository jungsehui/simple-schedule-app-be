package com.example.simplescheduleapp.support;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.ExceptionCode;
import org.junit.jupiter.api.function.Executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CommonExceptionTest extends MockTestSupport {

    public void testException(Executable executable, ExceptionCode e) {
        ExceptionCode code = assertThrows(ApplicationException.class, executable)
                .getCode();
        assertThat(code).isEqualTo(e);
    }
}
