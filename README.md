# Nukagit - making git distributed

## What is this?

Nukagit is my attempt at making a JGIT DFS based git server backed by a combination
of minio and mysql. It is a work in progress and is not ready for production use.

The idea is to run a cross-regional minio cluster and a group replicated instance
of mysql. This should result in very high availability and durability. And should also
provide good read performance in remote regions (group replication means latest data is
always available for reading). The write overhead of having to write to multiple
remote regions is not such a huge problem, as most people can survive an extra second
of latency when pushing to a remote git repository.

## Building

```shell
./gradlew build
```

## Running

```shell
docker-compose up
# you might have to run it a couple times because mysql fails to chown
# it's directory on a Mac with colima

./gradlew :run --args="migrate"
./gradlew :run --args="serve"
```

## Testing

There are two kinds of repositories supported at the moment:

- In-memory ones that are created on clone
- Minio/Mysql backed ones that you have top use grpc to create before cloning

First you will have to make a user:

```shell
./gradlew :cli:run --args="add_user username -F ../src/test/resources/fixtures/id_ecdsa.pub"
```

Feel free to use your own public key instead of the one in the fixtures directory.

To test an in-memory repository:

```shell
git clone "ssh://git@localhost:2222/memory/test-repository"
# Repository will be created automatically
# You will get a warning about connecting to a new ssh host, this is expected
# a new ssh host key will be generated in keys/ssh_host_key.pem
```

For a minio one create the repository using the cli and then clone it:

```shell
./gradlew :cli:run --args="create_repository testing"
git clone "ssh://git@localhost:2222/testing"
```