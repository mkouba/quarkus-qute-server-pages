package io.quarkiverse.quteserverpages.deployment;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkiverse.quteserverpages.runtime.QspBuildTimeConfig;
import io.quarkiverse.quteserverpages.runtime.QspExtensions;
import io.quarkiverse.quteserverpages.runtime.QspRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.qute.deployment.TemplateFilePathsBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;

class QspProcessor {

    private static final Logger LOG = Logger.getLogger(QspProcessor.class);

    private static final String FEATURE = "qsp";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        // QspExtensions is not a bean but we need to make it a part of the bean archive index
        return new AdditionalBeanBuildItem(QspExtensions.class);
    }

    @BuildStep
    public void collectTemplatePaths(TemplateFilePathsBuildItem templateFilePaths,
            QspBuildTimeConfig config, BuildProducer<QspTemplatePathBuildItem> paths) {
        for (String path : templateFilePaths.getFilePaths()) {
            if (config.hiddenTemplates.matcher(path).matches()) {
                LOG.debugf("Template %s is hidden", path);
            } else {
                paths.produce(new QspTemplatePathBuildItem(path));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    public RouteBuildItem produceTemplatesRoute(QspRecorder recorder, List<QspTemplatePathBuildItem> templatePaths,
            HttpRootPathBuildItem httpRootPath, QspBuildTimeConfig config) {
        return httpRootPath.routeBuilder()
                .routeFunction(httpRootPath.relativePath(config.rootPath + "/*"), recorder.initializeRoute())
                .handlerType(config.useBlockingHandler ? HandlerType.BLOCKING : HandlerType.NORMAL)
                .handler(recorder.handler(httpRootPath.relativePath(config.rootPath),
                        templatePaths.stream().map(QspTemplatePathBuildItem::getPath).collect(Collectors.toSet())))
                .build();
    }

    @BuildStep
    public HotDeploymentWatchedFileBuildItem watchFiles() {
        return HotDeploymentWatchedFileBuildItem.builder()
                .setLocationPredicate(f -> f.startsWith("templates"))
                .setRestartNeeded(true)
                .build();
    }
}
