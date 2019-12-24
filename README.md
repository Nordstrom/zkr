# zkr - ZooKeeper Reaper

Utility to view and restore ZooKeeper transactions from transaction log or Exhibitor backup files

## Background

This project provides a command-line utility that can be used to replay transactions from Exhibitor transaction log
and backup archive (gzip'd) logs.  Backup archives are automatically detected.

This utility ignores ephemeral znodes.

## Build

This project was developed with gradle 6.0.1, java 11 and kotlin 1.3.61

`gradle clean assemble`

This will create a shaded jar named `build/libs/zkr-all.jar`

## Usage

To see the available options, run:

`java -jar build/libs/zkr-all.jar --help`

The only required options is `-z`/`--zookeeper` which is a standard ZooKeeper connection string (e.g., localhost:2181)


### ZooKeeper Security via `jaas.conf`

Create a `jaas.conf` file with appropriate values. For example:

```shell script
Server {
  org.apache.zookeeper.server.auth.DigestLoginModule required
  user_admin="developer"
  user_developer="developer";
};
Client {
  org.apache.zookeeper.server.auth.DigestLoginModule required
  username="admin"
  password="developer";
};
```

Invoke `zkr` thusly:

`java -Djava.security.auth.login.config=./jaas.conf -cp build/libs/zkr-all.jar zkr.Zkr [options] log`

## Restoring Kafka

This tool can restore topics and acls in a Kafka cluster from the Exhibitor transaction log or backup
but must be done in a specific order:

- start Exhibitor/ZooKeeper (ONLY) with backups `disabled` (so you don't accidentally overwrite your backup files)
- Run `zkr` with `--overwrite-existing` option for all log files desired.
- start brokers, et al