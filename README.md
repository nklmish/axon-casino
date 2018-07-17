# axon-casino
Demo application illustrating how we can use CQRS + EventSourcing to build simple betting app using [AxonFramework](https://github.com/AxonFramework/AxonFramework)

# Dependencies
You need to get the trial license of vaadin charts using https://vaadin.com/pro/licenses

# Launch
`mvn vaadin:update-widgetset`
`mvn spring-boot:run`
`localhost:8080`

With the default configuration, it will use an embedded H2 database for all storage. Through profiles, MySQL and/or
AxonHub+DB can be used for a fully scalable configuration.
