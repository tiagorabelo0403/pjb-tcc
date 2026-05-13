package com.tcc.pjb.backend.core.storage;

import java.net.URI;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.tcc.pjb.backend.core.storage.local.LocalFilesystemObjectStorageAdapter;

@Configuration
@EnableConfigurationProperties(ObjectStorageProperties.class)
public class ObjectStorageConfiguration {

    @Bean
    public ObjectStoragePort objectStoragePort(ObjectStorageProperties props) {
        URI base = URI.create("http://localhost");
        try {
            base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
        } catch (Exception ignored) {
        }

        return new LocalFilesystemObjectStorageAdapter(
                Path.of(props.getLocal().getBaseDir()),
                base
        );
    }
}
