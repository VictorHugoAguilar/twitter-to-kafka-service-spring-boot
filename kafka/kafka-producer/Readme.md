# Resolution of possible failures

## Problems downloading avro serializer dependencies

- Add new config in pom.xml

```
<repository>
      <id>confluent</id>
      <url>http://packages.confluent.io/maven/</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
</repository>
```

- Add in setting config in .m2/setting.xml

```
<mirror>
    <id>confluent</id>
    <mirrorOf>confluent</mirrorOf>
    <name>Nexus public mirror</name>
    <url>http://packages.confluent.io/maven/</url>
</mirror>
```
