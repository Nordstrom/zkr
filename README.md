# zkr - ZooKeeper Reaper

Utility to backup/restore ZooKeeper znodes or view and restore ZooKeeper transactions from transaction log/Exhibitor backup files.

## Background

This project provides a command-line utility that can be used to backup and restore ZooKeeper znodes in JSON, optionally compressed, and write to a file or S3.

The `logs` command can replay transactions from Exhibitor transaction log and backup archive (gzip'd) logs. The `logs` command ignores ephemeral znodes.

## Usage

`./zkr help`

```shell script
ZooKeeper Reaper v0.3 - ZooKeeper backup/restore utility
Usage: zkr [-hV] [COMMAND]
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  help     Displays help information about the specified command
  backup   Backup ZooKeeper znodes
  logs     View/write ZooKeeper/Exhibitor transaction logs and backups
  restore  Restore ZooKeeper znodes from backup
```

`./zkr backup help`

```shell script
Usage: zkr backup [-cflv] [--pretty] [-m=<maxRetries>] [-p=<path>] [-r=<repeatMin>] [-s=<sessionTimeoutMs>]
                  [--s3-bucket=<s3bucket>] [--s3-region=<s3region>] [-z=<host>] [-e=<excludes>[,<excludes>...]]...
                  [-i=<includes>[,<includes>...]]... <file> [COMMAND]
Backup ZooKeeper znodes
      <file>               Transaction log or backup file
  -c, --compress           Compress output (default: false)
  -e, --exclude=<excludes>[,<excludes>...]
                           Comma-delimited list of paths to exclude (can be regex)
  -f, --ephemeral          Backup ephemeral znodes (default: false)
  -i, --include=<includes>[,<includes>...]
                           Comma-delimited list of paths to include (can be regex)
  -l, --not-leader         Perform backup/restore even if not ZooKeeper ensemble leader (default: false)
  -m, --max-retries=<maxRetries>
                           Maximum number of retries to read consistent data (default: 5)
  -p, --path=<path>        ZooKeeper root path for backup/restore (default: /)
      --pretty             Pretty print JSON output (default: false)
  -r, --repeat.min=<repeatMin>
                           Perform periodic backup every <repeatMin> minutes
  -s, --session-timeout-ms=<sessionTimeoutMs>
                           ZooKeeper session timeout in milliseconds (default: 30000)
      --s3-bucket=<s3bucket>
                           S3 bucket containing Exhibitor transaction logs/backups or zkr backup files
      --s3-region=<s3region>
                           AWS Region (default: us-west-2)
  -v, --verbose            Verbose (DEBUG) logging level (default: false)
  -z, --zookeeper=<host>   Target ZooKeeper host:port (default: localhost:2181)
Commands:
  help  Displays help information about the specified command
```

`./zkr restore help`

```shell script
Usage: zkr restore [-cdlov] [--info] [-p=<path>] [-s=<sessionTimeoutMs>] [--s3-bucket=<s3bucket>]
                   [--s3-region=<s3region>] [-z=<host>] [-e=<excludes>[,<excludes>...]]... [-i=<includes>[,
                   <includes>...]]... <file> [COMMAND]
Restore ZooKeeper znodes from backup
      <file>                 Transaction log or backup file
  -c, --compress             Compressed input (default: false)
  -d, --dry-run              Do not actually perform the actions (default: false)
  -e, --exclude=<excludes>[,<excludes>...]
                             Comma-delimited list of paths to exclude (can be regex)
  -i, --include=<includes>[,<includes>...]
                             Comma-delimited list of paths to include (can be regex)
      --info                 Print information about transaction log or backup then exit  (default: false)
  -l, --not-leader           Perform backup/restore even if not ZooKeeper ensemble leader (default: false)
  -o, --overwrite-existing   Overwrite existing znodes (default: false)
  -p, --path=<path>          ZooKeeper root path for backup/restore (default: /)
  -s, --session-timeout-ms=<sessionTimeoutMs>
                             ZooKeeper session timeout in milliseconds (default: 30000)
      --s3-bucket=<s3bucket> S3 bucket containing Exhibitor transaction logs/backups or zkr backup files
      --s3-region=<s3region> AWS Region (default: us-west-2)
  -v, --verbose              Verbose (DEBUG) logging level (default: false)
  -z, --zookeeper=<host>     Target ZooKeeper host:port (default: localhost:2181)
Commands:
  help  Displays help information about the specified command
```

`./zkr logs help`

```shell script
Usage: zkr logs [-cdlov] [--info] [-p=<path>] [-s=<sessionTimeoutMs>] [--s3-bucket=<s3bucket>] [--s3-region=<s3region>]
                [-z=<host>] [-e=<excludes>[,<excludes>...]]... [-i=<includes>[,<includes>...]]... <file> [COMMAND]
View/write ZooKeeper/Exhibitor transaction logs and backups
      <file>                 Transaction log or backup file
  -c, --compress             Compressed input (default: false)
  -d, --dry-run              Do not actually perform the actions (default: false)
  -e, --exclude=<excludes>[,<excludes>...]
                             Comma-delimited list of paths to exclude (can be regex)
  -i, --include=<includes>[,<includes>...]
                             Comma-delimited list of paths to include (can be regex)
      --info                 Print information about transaction log or backup then exit  (default: false)
  -l, --not-leader           Perform backup/restore even if not ZooKeeper ensemble leader (default: false)
  -o, --overwrite-existing   Overwrite existing znodes (default: false)
  -p, --path=<path>          ZooKeeper root path for backup/restore (default: /)
  -s, --session-timeout-ms=<sessionTimeoutMs>
                             ZooKeeper session timeout in milliseconds (default: 30000)
      --s3-bucket=<s3bucket> S3 bucket containing Exhibitor transaction logs/backups or zkr backup files
      --s3-region=<s3region> AWS Region (default: us-west-2)
  -v, --verbose              Verbose (DEBUG) logging level (default: false)
  -z, --zookeeper=<host>     Target ZooKeeper host:port (default: localhost:2181)
Commands:
  help  Displays help information about the specified command
```


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

`java -Djava.security.auth.login.config=./jaas.conf -cp build/libs/zkr-all.jar zkr.Zkr <command> <options>`

Or use the `zkr` script in the root directory.

## Restoring Kafka

This tool can restore topics and acls in a Kafka cluster from the Exhibitor transaction log/backup or a backup file
but must be done in a specific order:

- start Exhibitor/ZooKeeper (ONLY) with backups `disabled` (so you don't accidentally overwrite your backup files)
- Run `zkr` with `--overwrite-existing` option.
- start brokers, et al


# ATTRIBUTIONS
 - ZooKeeper transaction and Exhibitor backup view/restore is based on ZooKeeper [LogFormatter](https://github.com/apache/zookeeper/blob/master/zookeeper-server/src/main/java/org/apache/zookeeper/server/LogFormatter.java)
 - Backup/Restore is based on [zoocreeper](https://github.com/boundary/zoocreeper)
