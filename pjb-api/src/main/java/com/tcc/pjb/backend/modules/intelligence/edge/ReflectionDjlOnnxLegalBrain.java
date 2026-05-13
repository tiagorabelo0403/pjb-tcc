package com.tcc.pjb.backend.modules.intelligence.edge;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReflectionDjlOnnxLegalBrain implements LocalLegalBrain {

    private static final Logger log = LoggerFactory.getLogger(ReflectionDjlOnnxLegalBrain.class);

    private final PjbEdgeAiProperties props;
    private final ReentrantLock loadLock = new ReentrantLock();

    private volatile Object predictor;

    public ReflectionDjlOnnxLegalBrain(PjbEdgeAiProperties props) {
        this.props = props;
    }

    @Override
    public void load() {
        loadLock.lock();
        try {
            if (!props.enabled()) {
                throw new IllegalStateException("Edge AI desabilitado (pjb.edge-ai.enabled=false)");
            }
            if (predictor != null) return;

            try {
            
            
            
            
            
            

            Class<?> criteriaClazz = Class.forName("ai.djl.repository.zoo.Criteria");
            Class<?> criteriaBuilderClazz = Class.forName("ai.djl.repository.zoo.Criteria$Builder");
            Class<?> zooModelClazz = Class.forName("ai.djl.repository.zoo.ZooModel");

            
            Method builderMethod = criteriaClazz.getMethod("builder");
            Object builder = builderMethod.invoke(null);

            
            Method setTypes = criteriaBuilderClazz.getMethod("setTypes", Class.class, Class.class);
            builder = setTypes.invoke(builder, String.class, String.class);

            
            Method optModelPath = criteriaBuilderClazz.getMethod("optModelPath", Path.class);
            builder = optModelPath.invoke(builder, Path.of(props.modelPath()));

            
            Method optEngine = criteriaBuilderClazz.getMethod("optEngine", String.class);
            builder = optEngine.invoke(builder, "OnnxRuntime");

            
            Method build = criteriaBuilderClazz.getMethod("build");
            Object criteria = build.invoke(builder);

            
            Method loadModel = criteriaClazz.getMethod("loadModel");
            Object zooModel = loadModel.invoke(criteria);

            
            Method newPredictor = zooModelClazz.getMethod("newPredictor");
            this.predictor = newPredictor.invoke(zooModel);

            log.info("EDGE_AI_LOADED modelPath={}", props.modelPath());

            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("DJL/onnxruntime não está no classpath. Compile/execute com -Pedge-ai.", e);
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao carregar Edge AI (ONNX).", e);
            }
        } finally {
            loadLock.unlock();
        }
    }

    @Override
    public String predictDraft(String resumoProcesso) {
        if (!props.enabled()) {
            throw new IllegalStateException("Edge AI desabilitado (pjb.edge-ai.enabled=false)");
        }
        if (predictor == null) {
            load();
        }

        String prompt = props.promptPrefix() + (resumoProcesso == null ? "" : resumoProcesso);

        try {
            Method predict = predictor.getClass().getMethod("predict", Object.class);
            Object out = predict.invoke(predictor, prompt);
            return out != null ? out.toString() : "";
        } catch (Exception e) {
            throw new IllegalStateException("Falha na inferência Edge AI.", e);
        }
    }
}
