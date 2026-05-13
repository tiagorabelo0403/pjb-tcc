package com.tcc.pjb.backend.contracts.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.spring.spring6.PactVerificationSpring6Provider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.tcc.pjb.backend.controller.auth.PasskeyAuthController;
import com.tcc.pjb.backend.model.dto.security.PasskeyStartRequest;
import com.tcc.pjb.backend.model.dto.security.WebAuthnChallengeResponse;
import com.tcc.pjb.backend.service.auth.surface.WebAuthnSurfaceFacadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Provider("PjbAuthenticationProvider")
@PactFolder("src/test/resources/pacts/provider")
class PasskeyAuthControllerProviderContractTest {

    private final WebAuthnSurfaceFacadeService facadeService = mock(WebAuthnSurfaceFacadeService.class);
    private final PasskeyAuthController controller = new PasskeyAuthController(facadeService);

    @BeforeEach
    void setUp(PactVerificationContext context) {
        reset(facadeService);
        PactProviderSpring6Support.configure(context, controller);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpring6Provider.class)
    void verify(PactVerificationContext context, MockHttpServletRequestBuilder request) {
        PactProviderSpring6Support.applyJsonBody(context, request);
        context.verifyInteraction();
    }

    @State("passkey challenge can be generated")
    void passkeyChallengeCanBeGenerated() {
        when(facadeService.startPasskey(any(PasskeyStartRequest.class)))
                .thenReturn(new WebAuthnChallengeResponse(101L, "{\"challenge\":\"abc\",\"rpId\":\"pjb.test\"}"));
    }
}
