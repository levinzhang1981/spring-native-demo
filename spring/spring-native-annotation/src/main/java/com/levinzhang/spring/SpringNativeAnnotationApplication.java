package com.levinzhang.spring;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.nativex.hint.JdkProxyHint;
import org.springframework.nativex.hint.ResourceHint;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Supplier;

@SpringBootApplication
@ResourceHint(patterns = "Log4j-charsets.properties")
@TypeHint(types =  ReflectionRunner.Customer.class, access = { TypeAccess.DECLARED_CONSTRUCTORS, TypeAccess.DECLARED_METHODS })
@JdkProxyHint(types = {
        com.levinzhang.spring.ProxyRunner.Animal.class,
        org.springframework.aop.SpringProxy.class,
        org.springframework.aop.framework.Advised.class,
        org.springframework.core.DecoratingProxy.class
})
public class SpringNativeAnnotationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringNativeAnnotationApplication.class, args);
    }

}

@Component
class ReflectionRunner implements ApplicationRunner {

    private final ObjectMapper objectMapper ;

    ReflectionRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    record Customer(Integer id, String name) {
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var json = """
                [
                 { "id" : 2, "name": "Dr. Syer"} ,
                 { "id" : 1, "name": "Jürgen"} ,
                 { "id" : 4, "name": "Olga"} ,
                 { "id" : 3, "name": "Violetta"}  
                ]
                """;
        var result = this.objectMapper.readValue(json, new TypeReference<List<Customer>>() {
        });
        System.out.println("there are " + result.size() + " customers.");
        result.forEach(System.out::println);
    }
}

@Component
class ResourceRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var resource = new ClassPathResource("Log4j-charsets.properties");
        Assert.isTrue(resource.exists(), () -> "the file must exist");
        try (var in = new InputStreamReader(resource.getInputStream())) {
            var contents = FileCopyUtils.copyToString(in);
            System.out.println(contents.substring(0, 100) + System.lineSeparator() + "...");
        }
    }
}

@Component
class ProxyRunner implements ApplicationRunner {

    private static Animal buildAnimalProxy(Supplier<String> greetings) {
        var pfb = new ProxyFactoryBean();
        pfb.addInterface(Animal.class);
        pfb.addAdvice((MethodInterceptor) invocation -> {
            if (invocation.getMethod().getName().equals("speak"))
                System.out.println(greetings.get());

            return null;
        });
        return (Animal) pfb.getObject();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var cat = buildAnimalProxy(() -> "meow!");
        cat.speak();

        var dog = buildAnimalProxy(() -> "woof!");
        dog.speak();
    }

    interface Animal {
        void speak();
    }
}
