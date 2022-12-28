package com.myorg;

import software.constructs.Construct;

import java.util.HashMap;
import org.json.simple.JSONArray;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.Method;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.CfnFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

public class MysqljlambdaStack extends Stack {
    public MysqljlambdaStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public MysqljlambdaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Environment variable to separate the environments
        String environment = "dev";

        //Lambda Environment Variables to pass to the Lambdas
        HashMap<String, String> env = new HashMap <String, String>();
        env.put("ENVIRONMENT", environment);
        //MySQL RDS Configuration
        env.put("DBENDPOINT", "dbendpointforrdsmysl");
        env.put("DATABASENAME", "santasworkshop");
        env.put("USERNAME", "myusername");
        env.put("PASSWORD", "mypassword");
        
        //Graviton based Lambdas
        String key = "Architectures";
        JSONArray values = new JSONArray();
        values.add("arm64");

        int memorySize = 1024;
        
        Function getRDSDataLambdaFunction = Function.Builder.create(this, "getRDSDataLambda")
                .runtime(Runtime.JAVA_11)
                .functionName("getRDSDataLambda")
                .timeout(Duration.seconds(30))
                .memorySize(memorySize)
                .environment(env)
                .code(Code.fromAsset("target/mysqljlambda-0.1.jar"))
                .handler("com.myorg.lambda.GetRDSDataLambdaHandler::handleRequest")
                .build();
        //configure to run as a graviton2 lambda
        CfnFunction cfnFunction = (CfnFunction)getRDSDataLambdaFunction.getNode().getDefaultChild();
        cfnFunction.addPropertyOverride(key, values); 

        //API Gateway Configuration (Allowing Lambdas to be called via the API Gateway
        RestApi api = RestApi.Builder.create(this, "MySQL")
                .restApiName("MySQL").description("MySQL")
                .build();
        
        LambdaIntegration getRDSDataIntegration = LambdaIntegration.Builder.create(getRDSDataLambdaFunction) 
                .requestTemplates(new HashMap<String, String>() {{
                    put("application/json", "{ \"statusCode\": \"200\" }");
                }}).build();    
        
        //Get RDS
        Resource rdsResource = api.getRoot().addResource("rds");
        Method getRDSDataMethod = rdsResource.addMethod("GET", getRDSDataIntegration);        

        String urlPrefix = api.getUrl().substring(0, api.getUrl().length()-1);
        
        CfnOutput.Builder.create(this, "ZA GET RDS Lambda")
        .description("")
        .value("RDS Lambda:"+urlPrefix + getRDSDataMethod.getResource().getPath())
        .build();
    }
}
