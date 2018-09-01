package net.androidcart.rxbusretrofit;

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import net.androidcart.rxbusretrofitschema.API_STATE;
import net.androidcart.rxbusretrofitschema.RxBusRetrofitSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;


public class RxBusRetrofitProcessor extends AbstractProcessor {

    ProcessingEnvironment pe;
    private Filer filer;
    private Messager messager;
    private Elements elements;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        pe = processingEnv;
        filer = pe.getFiler();
        messager = pe.getMessager();
        elements = pe.getElementUtils();
    }


    private ClassName context(){
        return ClassName.get("android.content", "Context");
    }

    private ClassName rxBus(){
        return ClassName.get("net.androidcart.androidutils.eventbus", "RxBus");
    }

    private ClassName okHttpRequest(){
        return ClassName.get("okhttp3", "Request");
    }
    private ClassName retrofitResponse(){
        return ClassName.get("retrofit2", "Response");
    }

    private ClassName okHttpResponseBody(){
        return ClassName.get("okhttp3", "ResponseBody");
    }

    private ClassName retrofitCall(){
        return ClassName.get("retrofit2", "Call");
    }

    private ClassName consumer(){
        return ClassName.get("io.reactivex.functions", "Consumer");
    }






    public CodeBlock getCallApiCodeBlock(ClassName apiItemClassName) {
        CodeBlock.Builder block = CodeBlock.builder();
        block.addStatement("final " + okHttpRequest().withoutAnnotations().toString() + " request = call.request()");

        String itemName = apiItemClassName.simpleName();
        String state = API_STATE.class.getName();

        //TODO: call handler
        block.addStatement("if (apiHandler != null) apiHandler.onApiStart(type, request, startObj)");

        block.beginControlFlow("if (mapper == null)");
            block.addStatement("bus.publish(new " + itemName + "(" + state + ".START, type , startObj, request, startObj, null))");
        block.nextControlFlow(" else ");
            block.addStatement("try{mapper.accept(new " + itemName + "(" + state + ".START, type , startObj, request, startObj, null)); } catch(Exception ignored){}");
        block.endControlFlow();


        block.addStatement("failHelper.put(call, startObj)");

        block.add(
        "call.enqueue(new retrofit2.Callback() {" + "\n" +
        "   @Override" + "\n" +
        "   public void onResponse(Call call, retrofit2.Response response) {" + "\n" +
        "       failHelper.remove(call);" + "\n" +
        "       if(response.isSuccessful()) {" + "\n" +
        "           Object resBody = response.body();" + "\n" +
        "           if (apiHandler != null) apiHandler.onApiSuccess(type, resBody, response, request, startObj);" + "\n" +
        "           if (mapper == null) { " + "\n" +
        "               bus.publish(new " + itemName + "(" + state + ".SUCCESS, type, resBody, request, startObj, response));" + "\n" +
        "           } else { " + "\n" +
        "               try{mapper.accept(new " + itemName + "(" + state + ".SUCCESS, type, resBody, request, startObj, response)); } catch(Exception ignored){}" + "\n" +
        "           } " + "\n" +
        "           " + "\n" +
        "       } else {" + "\n" +
        "           okhttp3.ResponseBody errBody = response.errorBody();" + "\n" +
        "           if (apiHandler != null) apiHandler.onApiFailure(type, errBody, response, request, startObj);" + "\n" +
        "           if (mapper == null) { " + "\n" +
        "               bus.publish(new " + itemName + "(" + state + ".FAILURE, type, errBody, request, startObj, response));" + "\n" +
        "           } else { " + "\n" +
        "               try{mapper.accept(new " + itemName + "(" + state + ".FAILURE, type, errBody, request, startObj, response)); } catch(Exception ignored){}" + "\n" +
        "           } " + "\n" +
        "           " + "\n" +
        "       }" + "\n" +
        "   }" + "\n" +


        "   @Override" + "\n" +
        "   public void onFailure(Call call, Throwable t) {" + "\n" +
        "       Object startObj = failHelper.remove(call);" + "\n" +
        "       apiHandler.onGeneralFailure(type, t, request, startObj);" + "\n" +
        "       if (mapper == null) { " + "\n" +
        "           bus.publish(new " + itemName + "(" + state + ".FAILURE, type, t, request, startObj, null));" + "\n" +
        "       } else { " + "\n" +
        "           try{mapper.accept(new " + itemName + "(" + state + ".FAILURE, type, t, request, startObj, null)); } catch(Exception ignored){}" + "\n" +
        "       } " + "\n" +
        "   }" + "\n" +
        "});" + "\n" +

        "");


        return block.build();
    }




    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        for (Element element : roundEnvironment.getElementsAnnotatedWith(RxBusRetrofitSchema.class)) {

            if (element.getKind() != ElementKind.INTERFACE) {
                messager.printMessage(Diagnostic.Kind.WARNING, "RxBusRetrofitSchema must be an retrofit schema interface");
                continue;
            }

            TypeElement typeElement = (TypeElement) element;
            String packageName = elements.getPackageOf(typeElement).getQualifiedName().toString();
            RxBusRetrofitSchema annot = typeElement.getAnnotation(RxBusRetrofitSchema.class);
            String schemaName = typeElement.getSimpleName().toString();


            TypeMirror apiHandlerClass = null;
            try
            {
                annot.handler();
            }
            catch( MirroredTypeException mte )
            {
                apiHandlerClass = mte.getTypeMirror();
            }

//            ApiHandler apiHandler = new ApiHandler();
//            for (Constructor constructor : apiHandlerClass.getConstructors()){
//                if ( constructor.isAccessible() && constructor.getParameterCount() == 0 ){
//                    try {
//                        Object apiHandlerObj = constructor.newInstance();
//                        if (apiHandlerObj instanceof ApiHandler){
//                            apiHandler = (ApiHandler) apiHandlerObj;
//                        }
//                    } catch (Throwable t) {
//                        t.printStackTrace();
//                    }
//                }
//            }

            ClassName schemaClass = ClassName.get(packageName, schemaName);

            ClassName apiTypeClassName = ClassName.get(packageName, schemaName + "ApiType");
            ClassName apiItemClassName = ClassName.get(packageName, schemaName + "ApiItem");

            String APICallbackMethodsName = schemaName + "Callback"; //"CallbackMethods" ;
            ClassName APICallbackMethodsMethod = ClassName.get(packageName, APICallbackMethodsName);
            TypeName APICallbackMethodsMethodType = APICallbackMethodsMethod.withoutAnnotations();

            TypeName rxBusWithApiItemTN = ParameterizedTypeName.get(rxBus(), apiItemClassName);
            TypeName consumerWithApiItemTN = ParameterizedTypeName.get(consumer(), apiItemClassName);

            ClassName apiHandlerClassName = ClassName.get(packageName, schemaName + "ApiHandler");


            String APICallbackMapperName = schemaName + "CallbackMapper";
            ClassName APICallbackMapperClassName = ClassName.get(packageName, APICallbackMapperName);

            //Publisher
            TypeSpec.Builder publisher = TypeSpec
                    .classBuilder(schemaName + "Publisher")
                    .addModifiers(Modifier.PUBLIC)
                    .addField(rxBusWithApiItemTN, "bus")
                    .addField(schemaClass, "api")
                    .addField(apiHandlerClassName, "apiHandler")
                    .addField(
                            FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(HashMap.class), retrofitCall(), ClassName.get(Object.class)), "failHelper")
                                    .initializer("new HashMap<>()")
                                    .build()
                    )
                    ;

            MethodSpec.Builder publisherConstructorBuilder = MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(schemaClass, "api")
                    .addStatement("this.api = api")
                    .addStatement("this.bus = new RxBus<>()");
            if (apiHandlerClass != null && apiHandlerClass.getKind() != TypeKind.VOID) {
                publisherConstructorBuilder.addStatement("this.apiHandler = new " + apiHandlerClass.toString() + "()");
            };
            publisher.addMethod(publisherConstructorBuilder.build());

            publisher.addMethod( MethodSpec.methodBuilder("subscribe")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(consumerWithApiItemTN, "action")
                    .beginControlFlow("try")
                            .addStatement("bus.subscribe(action)")
                    .endControlFlow()
                    .beginControlFlow("catch(Exception ignored)")
                    .endControlFlow()
            .build());

            publisher.addMethod( MethodSpec.methodBuilder("unregister")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(consumerWithApiItemTN, "action")
                    .beginControlFlow("try")
                        .addStatement("bus.unregister(action)")
                    .endControlFlow()
                    .beginControlFlow("catch(Exception ignored)")
                    .endControlFlow()
                    .build());





            publisher.addMethod( MethodSpec.methodBuilder("callApi").addModifiers(Modifier.PROTECTED)
                    .addParameter(apiTypeClassName, "type")
                    .addParameter(retrofitCall(), "call")
                    .addStatement("callApi(null, type, call, null)")
                    .build());


            publisher.addMethod( MethodSpec.methodBuilder("callApi").addModifiers(Modifier.PROTECTED)
                    .addParameter(apiTypeClassName, "type", Modifier.FINAL)
                    .addParameter(retrofitCall(), "call", Modifier.FINAL)
                    .addParameter(Object.class, "startObj", Modifier.FINAL)
                    .addStatement("callApi(null, type, call, startObj)")
                    .build());


            publisher.addMethod( MethodSpec.methodBuilder("callApi").addModifiers(Modifier.PROTECTED)
                    .addParameter(APICallbackMapperClassName, "mapper", Modifier.FINAL)
                    .addParameter(apiTypeClassName, "type", Modifier.FINAL)
                    .addParameter(retrofitCall(), "call", Modifier.FINAL)
                    .addStatement("callApi(mapper, type, call, null)", Modifier.FINAL)
                    .build());


            publisher.addMethod( MethodSpec.methodBuilder("callApi").addModifiers(Modifier.PROTECTED)
                    .addParameter(APICallbackMapperClassName, "mapper", Modifier.FINAL)
                    .addParameter(apiTypeClassName, "type", Modifier.FINAL)
                    .addParameter(retrofitCall(), "call", Modifier.FINAL)
                    .addParameter(Object.class, "startObj", Modifier.FINAL)
                    .addCode(getCallApiCodeBlock(apiItemClassName))
                    .build());



            //ApiItem
            TypeSpec.Builder apiItem = TypeSpec
                    .classBuilder(schemaName + "ApiItem")
                    .addModifiers(Modifier.PUBLIC)
                    .addField(API_STATE.class, "state")
                    .addField(apiTypeClassName, "type")
                    .addField(Object.class, "o")
                    .addField(okHttpRequest(), "request")
                    .addField(Object.class, "startObject")
                    .addField(retrofitResponse(), "response");

            MethodSpec apiItemConstructor = MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(API_STATE.class, "state")
                    .addParameter(apiTypeClassName, "type")
                    .addParameter(Object.class, "o")
                    .addParameter(okHttpRequest(), "request")
                    .addParameter(Object.class, "startObject")
                    .addParameter(retrofitResponse(), "response")
                    .addStatement("this.state = state")
                    .addStatement("this.type = type")
                    .addStatement("this.o = o")
                    .addStatement("this.request = request")
                    .addStatement("this.startObject = startObject")
                    .addStatement("this.response = response")
                    .build();
            apiItem.addMethod(apiItemConstructor);

            //ApiType
            TypeSpec.Builder apiType = TypeSpec
                    .enumBuilder(schemaName + "ApiType" )
                    .addModifiers(Modifier.PUBLIC);



            //ApiHandler
            TypeSpec.Builder apiHandlerBuilder = TypeSpec
                    .classBuilder(apiHandlerClassName )
                    .addModifiers(Modifier.PUBLIC)

                    .addMethod(MethodSpec.methodBuilder("onApiStart")
                        .addParameter(apiTypeClassName, "type")
                        .addParameter(okHttpRequest(), "request")
                        .addParameter(Object.class, "startObject")
                        .build())

                    .addMethod(MethodSpec.methodBuilder("onApiSuccess")
                            .addParameter(apiTypeClassName, "type")
                            .addParameter(Object.class, "body")
                            .addParameter(retrofitResponse(), "response")
                            .addParameter(okHttpRequest(), "request")
                            .addParameter(Object.class, "startObject")
                            .build())

                    .addMethod(MethodSpec.methodBuilder("onApiFailure")
                            .addParameter(apiTypeClassName, "type")
                            .addParameter(okHttpResponseBody(), "errorBody")
                            .addParameter(retrofitResponse(), "response")
                            .addParameter(okHttpRequest(), "request")
                            .addParameter(Object.class, "startObject")
                            .build())

                    .addMethod(MethodSpec.methodBuilder("onGeneralFailure")
                            .addParameter(apiTypeClassName, "type")
                            .addParameter(Throwable.class, "throwable")
                            .addParameter(okHttpRequest(), "request")
                            .addParameter(Object.class, "startObject")
                            .build());

            //APICallbackMethods
            TypeSpec.Builder apiMethods = TypeSpec
                    .classBuilder( APICallbackMethodsName )
                    .addModifiers(Modifier.PUBLIC);

            //APICallbackMapper

            TypeSpec.Builder apiMapper = TypeSpec
                    .classBuilder(APICallbackMapperName )
                    .addModifiers(Modifier.PUBLIC)
                    .addField(FieldSpec.builder(APICallbackMethodsMethodType, "methods").addModifiers(Modifier.PRIVATE).build());

            apiMapper.addSuperinterface( consumerWithApiItemTN );


            MethodSpec mapperConstructor = MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(APICallbackMethodsMethodType, "methods")
                    .addStatement("this.methods = methods")
                    .build();
            apiMapper.addMethod(mapperConstructor);

            MethodSpec.Builder mapperAccept = MethodSpec
                    .methodBuilder("accept")
                    .addParameter(apiItemClassName, "res")
                    .addException(Exception.class)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC);






            mapperAccept.beginControlFlow("switch ( res.type )");

            for ( Element method : typeElement.getEnclosedElements() ) {
                if (method.getKind() != ElementKind.METHOD) {
                    continue;
                }
                String name = method.getSimpleName().toString();
                ExecutableElement eMethod = (ExecutableElement) method;
                DeclaredType type = (DeclaredType)eMethod.getReturnType();

                if ( type.getTypeArguments().size()>0 ){
                    type = (DeclaredType)type.getTypeArguments().get(0);
                }

                //messager.printMessage(Diagnostic.Kind.NOTE, "aminclass: " + type );

                apiType.addEnumConstant(name);




                String typeNameSimple = type.asElement().getSimpleName().toString();
                String typeNameFull = type.toString();

                String typeCamel = typeNameSimple;
                if ( typeCamel.length()>1 ) {
                    typeCamel = typeCamel.substring(0, 1).toLowerCase() + typeCamel.substring(1);
                } else {
                    typeCamel = typeCamel.toLowerCase();
                }
                typeCamel = "result"; //XXX: Boolean -> boolean => raises error

                MethodSpec callbackStartMethod = MethodSpec
                        .methodBuilder(name + "Start")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class)
                        .addParameter(Object.class, "object")
                        .addParameter(okHttpRequest(), "request")
                        .build();
                MethodSpec callbackSuccessMethod = MethodSpec
                        .methodBuilder(name + "Result")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class)
                        .addParameter(TypeName.get(type), typeCamel)
                        .addParameter(okHttpRequest(), "request")
                        .addParameter(Object.class, "startObject")
                        .addParameter(retrofitResponse(), "response")
                        .build();
                MethodSpec callbackFailureMethod = MethodSpec
                        .methodBuilder(name + "Failure")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class)
                        .addParameter(Object.class, "error")
                        .addParameter(okHttpRequest(), "request")
                        .addParameter(Object.class, "startObject")
                        .addParameter(retrofitResponse(), "response")
                        //.addStatement("return new $T($L, $L)", classIntent, "context", classClass + ".class")
                        .build();

                apiMethods.addMethod(callbackStartMethod);
                apiMethods.addMethod(callbackSuccessMethod);
                apiMethods.addMethod(callbackFailureMethod);






                mapperAccept
                        .addCode("case "+name+":\n")
                        .beginControlFlow("switch (res.state)")

                        .addCode("case START:\n")
                        .addStatement("methods."+name+"Start(res.o, res.request)")
                        .addStatement("break")

                        .addCode("case SUCCESS:\n")
                        .addStatement("methods."+name+"Result(("+ typeNameFull +")res.o, res.request, res.startObject, res.response)")
                        .addStatement("break")

                        .addCode("case FAILURE:\n")
                        .addStatement("methods."+name+"Failure(res.o, res.request, res.startObject, res.response)")
                        .addStatement("break")

                        .endControlFlow()
                        .addStatement("break")
                ;



                final String startObjectName = "startObject"; //"startObjectNameHelperLongName";
                MethodSpec.Builder hrMethod = MethodSpec
                        .methodBuilder(name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class) ;
                List<String> paramsNames = new ArrayList<>();
                for (VariableElement ve : eMethod.getParameters() ){
                    hrMethod.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString());
                    paramsNames.add(ve.getSimpleName().toString());
                }
                hrMethod.addParameter(Object.class, startObjectName);
                hrMethod.addStatement(String.format("callApi(%sApiType.%s, api.%s(%s), %s)", schemaName, name, name, String.join(",", paramsNames ), startObjectName));


                MethodSpec.Builder hrMethodSimple = MethodSpec
                        .methodBuilder(name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class) ;
                for (VariableElement ve : eMethod.getParameters() ){
                    hrMethodSimple.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString());
                }
                hrMethodSimple.addStatement(String.format("callApi(%sApiType.%s, api.%s(%s))", schemaName, name, name, String.join(",", paramsNames )));



                MethodSpec.Builder hrMethodNoBroadcast = MethodSpec
                        .methodBuilder(name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class) ;
                hrMethodNoBroadcast.addParameter(APICallbackMapperClassName, "mapper");
                for (VariableElement ve : eMethod.getParameters() ){
                    hrMethodNoBroadcast.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString());
                }
                hrMethodNoBroadcast.addParameter(Object.class, startObjectName);
                hrMethodNoBroadcast.addStatement(String.format("callApi(mapper, %sApiType.%s, api.%s(%s), %s)", schemaName, name, name, String.join(",", paramsNames ), startObjectName));



                MethodSpec.Builder hrMethodNoBroadcastSimple = MethodSpec
                        .methodBuilder(name)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(void.class) ;
                hrMethodNoBroadcastSimple.addParameter(APICallbackMapperClassName, "mapper");
                for (VariableElement ve : eMethod.getParameters() ){
                    hrMethodNoBroadcastSimple.addParameter(TypeName.get(ve.asType()), ve.getSimpleName().toString());
                }
                hrMethodNoBroadcastSimple.addStatement(String.format("callApi(mapper, %sApiType.%s, api.%s(%s))", schemaName, name, name, String.join(",", paramsNames )));



                publisher.addMethod(hrMethod.build());
                publisher.addMethod(hrMethodSimple.build());
                publisher.addMethod(hrMethodNoBroadcast.build());
                publisher.addMethod(hrMethodNoBroadcastSimple.build());

            }

            mapperAccept.endControlFlow();

//            MethodSpec intentMethod = MethodSpec
//                    .methodBuilder(METHOD_PREFIX + className)
//                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//                    .returns(classIntent)
//                    .addParameter(classContext, "context")
//                    .addStatement("return new $T($L, $L)", classIntent, "context", classClass + ".class")
//                    .build();
//            apiType.addMethod(intentMethod);

            try {
                JavaFile.builder(packageName, apiItem.build())
                        .build()
                        .writeTo(filer);
                JavaFile.builder(packageName, apiType.build())
                        .build()
                        .writeTo(filer);
                JavaFile.builder(packageName, apiMethods.build())
                        .build()
                        .writeTo(filer);
                apiMapper.addMethod(mapperAccept.build());
                JavaFile.builder(packageName, apiMapper.build())
                        .build()
                        .writeTo(filer);

                JavaFile.builder(packageName, publisher.build())
                        .build()
                        .writeTo(filer);

                JavaFile.builder(packageName, apiHandlerBuilder.build())
                        .build()
                        .writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        return true;
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(RxBusRetrofitSchema.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

}

