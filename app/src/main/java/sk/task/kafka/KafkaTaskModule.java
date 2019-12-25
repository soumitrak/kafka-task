package sk.task.kafka;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import net.sourceforge.argparse4j.inf.Namespace;
import sk.task.exec.TaskExecutorService;
import sk.task.exec.TaskExecutorServiceImpl;
import sk.task.exec.TaskLocator;
import sk.task.exec.TaskLocatorService;

public class KafkaTaskModule extends AbstractModule {
    private final Namespace ns;

    public KafkaTaskModule(final Namespace ns) {
        this.ns = ns;
    }

    @Override
    protected void configure() {
        GuiceDebug.enable();

        bind(TaskLocator.class).to(TaskLocatorService.class);
        bind(TaskExecutorService.class).to(TaskExecutorServiceImpl.class);
        bindConstant().annotatedWith(Names.named("NUM_THREADS")).to(ns.getInt("nt"));
        bind(Namespace.class).annotatedWith(Names.named("NS")).toInstance(ns);
    }
}