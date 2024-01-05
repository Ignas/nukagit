package lt.pow.nukagit.cli;

import lt.pow.nukagit.proto.Users;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "add_user", description = "Add user")
public class AddUser implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main parent;

    @CommandLine.Parameters(index = "0", description = "Username to add.")
    private String username;

    @CommandLine.Option(names = {"-k", "--key"}, description = "Specify a key (exclusive with --key-file)")
    private String keyString;

    @CommandLine.Option(names = {"-F", "--key-file"}, description = "Specify a key file (exclusive with --key)")
    private File keyFile;

    @Override
    public Integer call() {
        if (keyString != null && keyFile != null) {
            System.err.println("Cannot specify both --key and --key-file");
            return 1;
        } else if (keyString == null && keyFile == null) {
            System.err.println("Must specify either --key or --key-file");
            return 1;
        }

        if (keyFile != null) {
            // read key from file
            byte[] keyBytes;
            try {
                keyBytes = Files.readAllBytes(keyFile.toPath());
            } catch (IOException e) {
                System.err.println("Failed to read key file: " + e.getMessage());
                return 1;
            }
            keyString = new String(keyBytes, StandardCharsets.UTF_8);
        }

        System.out.println("Adding User " + username);
        parent.usersGrpcClient().createUser(Users.CreateUserRequest.newBuilder()
                .setUsername(username)
                .setPublicKey(keyString)
                .build());
        System.out.println("User added");
        return 0;
    }
}
