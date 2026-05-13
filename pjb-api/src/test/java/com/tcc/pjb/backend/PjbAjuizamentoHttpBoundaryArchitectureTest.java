package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.command.AjuizarProcessoCommand;
import com.tcc.pjb.backend.controller.ProcessoCommandController;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.mapper.ProcessoMapper;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.service.ProcessoResponseAssemblerService;
import com.tcc.pjb.backend.service.ajuizamento.ProcessoCommandSurfaceFacadeService;
import com.tcc.pjb.backend.service.document.SmartFileSplitter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbAjuizamentoHttpBoundaryArchitectureTest {

    @Test
    void processoCommandControllerDeveDelegarBoundaryMultipartParaSurfaceFacadeDedicada() {
        List<Class<?>> fieldTypes = List.of(ProcessoCommandController.class.getDeclaredFields()).stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getType)
                .toList();

        assertThat(fieldTypes)
                .containsExactly(ProcessoCommandSurfaceFacadeService.class)
                .doesNotContain(
                        AjuizarProcessoCommand.class,
                        SmartFileSplitter.class,
                        ProcessoMapper.class,
                        ProcessoResponseAssemblerService.class,
                        CurrentUserService.class
                );
    }

    @Test
    void processoRequestDeveCarregarFlagDeJuizo100DigitalNoBoundaryHttp() throws Exception {
        Field field = ProcessoRequest.class.getDeclaredField("juizo100Digital");
        assertThat(field.getType()).isEqualTo(boolean.class);
    }
}
