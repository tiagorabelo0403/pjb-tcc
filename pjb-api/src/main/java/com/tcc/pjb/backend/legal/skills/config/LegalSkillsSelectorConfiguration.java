package com.tcc.pjb.backend.legal.skills.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.tcc.pjb.backend.legal.skills.router.LegalSkillVersionSelector;
import com.tcc.pjb.backend.legal.skills.v1.LegalSkillRegistryV1;
import com.tcc.pjb.backend.legal.skills.v1.LegalSkillV1;
import com.tcc.pjb.backend.legal.skills.v2.LegalSkillRegistryV2;
import com.tcc.pjb.backend.legal.skills.v2.LegalSkillV2;
import com.tcc.pjb.backend.legal.skills.v3.LegalSkillRegistryV3;
import com.tcc.pjb.backend.legal.skills.v3.LegalSkillV3;

@Configuration
public class LegalSkillsSelectorConfiguration {

    @Bean
    public LegalSkillRegistryV1 legalSkillRegistryV1(ObjectProvider<LegalSkillV1> skills) {
        List<LegalSkillV1> list = new ArrayList<>(skills.orderedStream().toList());
        return new LegalSkillRegistryV1(list);
    }

    @Bean
    public LegalSkillRegistryV2 legalSkillRegistryV2(ObjectProvider<LegalSkillV2> skills) {
        List<LegalSkillV2> list = new ArrayList<>(skills.orderedStream().toList());
        return new LegalSkillRegistryV2(list);
    }

    @Bean
    public LegalSkillRegistryV3 legalSkillRegistryV3(ObjectProvider<LegalSkillV3> skills) {
        List<LegalSkillV3> list = new ArrayList<>(skills.orderedStream().toList());
        return new LegalSkillRegistryV3(list);
    }

    @Bean
    public LegalSkillVersionSelector legalSkillVersionSelector(LegalSkillRegistryV1 v1,
                                                               LegalSkillRegistryV2 v2,
                                                               LegalSkillRegistryV3 v3) {
        return new LegalSkillVersionSelector(v1, v2, v3);
    }
}
