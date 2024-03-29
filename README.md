# zkr - ZooKeeper Reaper

Utility to backup/restore ZooKeeper znodes or view and restore ZooKeeper transactions from transaction log/Exhibitor backup files.

## Background

This project provides a command-line utility that can be used to backup and restore ZooKeeper znodes into JSON, optionally compressed, and write to a file or S3.

The `logs` command can replay transactions from Exhibitor transaction log and backup archive (gzip'd) logs. The `logs` command ignores ephemeral znodes.

## Usage

### Commands

```
ZooKeeper Reaper v0.5 - ZooKeeper backup/restore utility
Usage: zkr [-hV] [COMMAND]
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  help     Displays help information about the specified command
  backup   Backup ZooKeeper znodes
  logs     View/write ZooKeeper/Exhibitor transaction logs and backups
  restore  Restore ZooKeeper znodes from backup
```

### Backup command

```
Usage: zkr backup [-cdflv] [--pretty] [-m=<maxRetries>] [-p=<path>] [-r=<repeatMin>] [-s=<sessionTimeoutMs>]
                  [--s3-bucket=<s3bucket>] [--s3-region=<s3region>] [--superdigest-password=<superDigestPassword>]
                  [-z=<host>] [-e=<excludes>[,<excludes>...]]... [-i=<includes>[,<includes>...]]... <file> [COMMAND]
Backup ZooKeeper znodes
      <file>               Transaction log or backup file
  -c, --compress           Compress output (default: false)
  -d, --dry-run            Do not actually perform the actions (default: false)
  -e, --exclude=<excludes>[,<excludes>...]
                           Comma-delimited list of paths to exclude (regex)
  -f, --ephemeral          Backup ephemeral znodes (default: false)
  -i, --include=<includes>[,<includes>...]
                           Comma-delimited list of paths to include (regex)
  -l, --not-leader         Perform backup/restore even if ZooKeeper is not ensemble leader or standalone (i.e.,
                             follower) (default: false)
  -m, --max-retries=<maxRetries>
                           Maximum number of retries to read consistent data (default: 5)
  -p, --path=<path>        ZooKeeper root path for backup/restore (default: /)
      --pretty             Pretty print JSON output (default: false)
  -r, --repeat-every=<repeatMin>
                           Perform periodic backup every <repeatMin> minutes
  -s, --session-timeout-ms=<sessionTimeoutMs>
                           ZooKeeper session timeout in milliseconds (default: 30000)
      --s3-bucket=<s3bucket>
                           S3 bucket containing Exhibitor transaction logs/backups or zkr backup files
      --s3-region=<s3region>
                           AWS Region (default: us-west-2)
      --superdigest-password=<superDigestPassword>
                           ZooKeeper superdigest password. ZKR_SUPERDIGEST_PASSWORD environment variable if found.
  -v, --verbose            Verbose (DEBUG) logging level (default: false)
  -z, --zookeeper=<host>   Target ZooKeeper host:port (default: localhost:2181)
Commands:
  help  Displays help information about the specified command
```

By default, backup will only execute if `--zookeeper` is the `leader` of an ensemble or `standalone`.  To backup a `follower` specify `--not-leader`.


### Restore command

```
Usage: zkr restore [-cdlov] [--info] [-p=<path>] [-s=<sessionTimeoutMs>] [--s3-bucket=<s3bucket>]
                   [--s3-region=<s3region>] [--superdigest-password=<superDigestPassword>] [-z=<host>] [-e=<excludes>[,
                   <excludes>...]]... [-i=<includes>[,<includes>...]]... <file> [COMMAND]
Restore ZooKeeper znodes from backup
      <file>                 Transaction log or backup file
  -c, --compress             Compressed input (default: false)
  -d, --dry-run              Do not actually perform the actions (default: false)
  -e, --exclude=<excludes>[,<excludes>...]
                             Comma-delimited list of paths to exclude (regex)
  -i, --include=<includes>[,<includes>...]
                             Comma-delimited list of paths to include (regex)
      --info                 Print information about transaction log or backup then exit  (default: false)
  -l, --not-leader           Perform backup/restore even if ZooKeeper is not ensemble leader or standalone (i.e.,
                               follower) (default: false)
  -o, --overwrite-existing   Overwrite existing znodes (default: false)
  -p, --path=<path>          ZooKeeper root path for backup/restore (default: /)
  -s, --session-timeout-ms=<sessionTimeoutMs>
                             ZooKeeper session timeout in milliseconds (default: 30000)
      --s3-bucket=<s3bucket> S3 bucket containing Exhibitor transaction logs/backups or zkr backup files
      --s3-region=<s3region> AWS Region (default: us-west-2)
      --superdigest-password=<superDigestPassword>
                             ZooKeeper superdigest password. ZKR_SUPERDIGEST_PASSWORD environment variable if found.
  -v, --verbose              Verbose (DEBUG) logging level (default: false)
  -z, --zookeeper=<host>     Target ZooKeeper host:port (default: localhost:2181)
Commands:
  help  Displays help information about the specified command
```

By default, restore will only execute if `--zookeeper` is the `leader` of an ensemble or 'standalone'.  To restore to a `follower` specify `--not-leader`.
Use `--dry-run` to see what would be restored without actually writing the znodes.


### Logs command

```
Usage: zkr logs [-lorv] [--info] [-p=<path>] [-s=<sessionTimeoutMs>] [--s3-bucket=<s3bucket>] [--s3-region=<s3region>]
                [--superdigest-password=<superDigestPassword>] [-z=<host>] [-e=<excludes>[,<excludes>...]]...
                [-i=<includes>[,<includes>...]]... <file> [COMMAND]
View/write ZooKeeper/Exhibitor transaction logs and backups
      <file>                 Transaction log or backup file
  -e, --exclude=<excludes>[,<excludes>...]
                             Comma-delimited list of paths to exclude (regex)
  -i, --include=<includes>[,<includes>...]
                             Comma-delimited list of paths to include (regex)
      --info                 Print information about transaction log or backup then exit  (default: false)
  -l, --not-leader           Perform backup/restore even if ZooKeeper is not ensemble leader or standalone (i.e.,
                               follower) (default: false)
  -o, --overwrite-existing   Overwrite existing znodes (default: false)
  -p, --path=<path>          ZooKeeper root path for backup/restore (default: /)
  -r, --restore              Execute (restore) transactions (default: false)
  -s, --session-timeout-ms=<sessionTimeoutMs>
                             ZooKeeper session timeout in milliseconds (default: 30000)
      --s3-bucket=<s3bucket> S3 bucket containing Exhibitor transaction logs/backups or zkr backup files
      --s3-region=<s3region> AWS Region (default: us-west-2)
      --superdigest-password=<superDigestPassword>
                             ZooKeeper superdigest password. ZKR_SUPERDIGEST_PASSWORD environment variable if found.
  -v, --verbose              Verbose (DEBUG) logging level (default: false)
  -z, --zookeeper=<host>     Target ZooKeeper host:port (default: localhost:2181)
Commands:
  help  Displays help information about the specified command
```

By default, the `logs` command will only display the log transactions.  Use `--restore` to actually write or overwrite znodes.


## Build

This project was developed with gradle 6.7.1, java 11 and kotlin 1.4.20

`./gradlew clean assemble`

This will create a shaded jar named `build/libs/zkr-all.jar`

## Usage

To see the available options, run:

`java -jar build/libs/zkr-all.jar --help`

The only required options is `-z`/`--zookeeper` which is a standard ZooKeeper connection string (e.g., localhost:2181)

## Backup/restore Kafka

### Backup
This tool can backup topics and acls in a Kafka cluster from either the Exhibitor transaction log/backup files.
It is very important to NOT backup broker ephemeral nodes:
- /kafka/controller
- /kafka/brokers/ids/

This is done by default as `--ephemeral`'s default value `false`

### Restore
Restoring can be done using Exhibitor/ZooKeeper transaction logs or backup files or a `zkr` backup file but must be done in a specific order:

- start Exhibitor/ZooKeeper (ONLY) with backups `disabled` (so you don't accidentally overwrite your backup files)
- Run `zkr` with `--overwrite-existing` option
- start brokers, et al

## Quick demo

Run a quick demo to recover Kafka metadata stored in ZooKeeper

### Create a Kafka cluster and topic

- Run `docker-compose up`
- Create a topic by running: `echo "hello" | kafkacat -b localhost:9092 -P -t hello`

### Take a backup, then tear it all down

- Take a backup: `./zkr.sh backup --zookeeper localhost:2181 -l backup-file`
- Tear it all down: `docker-compose down`

### Restart cluster using the backup

- Run `docker-compose up --no-start`
- Run `docker-compose start zookeeper`
- Restore ZooKeeper by running: `./zkr.sh restore --zookeeper localhost:2181 -l backup.json`
- Start Kafka cluster: `docker-compose start kafka-broker`

Check the restore by listing Kafka topics: `kafkacat -b localhost:9092 -L`

## ZooKeeper security

### via `jaas.conf`

Create a `jaas.conf` file with appropriate values. For example:

```
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

`java -Djava.security.auth.login.config=./jaas.conf -cp build/libs/zkr-all.jar zkr.Zkr <command> <options>`

Or use the `zkr` script in the root directory.

### `superdigest` support

It may be necessary to add `superdigest` authorization to access restricted znodes (e.g., `/kafka/config/users`)

This is supported by specifying the `superdigest` password in an environment variable `ZKR_SUPERDIGEST_PASSWORD` or using the `--superdigest-password` parameter.


## ATTRIBUTIONS
This utility borrowed heavily from:

 - ZooKeeper transaction and Exhibitor backup view/restore is based on ZooKeeper [LogFormatter](https://github.com/apache/zookeeper/blob/master/zookeeper-server/src/main/java/org/apache/zookeeper/server/LogFormatter.java)
 - Backup/Restore is based on [zoocreeper](https://github.com/boundary/zoocreeper)
