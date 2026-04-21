package com.example.blogger.util;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

import java.util.Scanner;

public class JasyptEncryptor {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("========================================");
        System.out.println("Jasypt 密码加密工具");
        System.out.println("========================================");

        String secret;
        String plain;

        if (args.length >= 2) {
            secret = args[0];
            plain = args[1];
        } else {
            System.out.print("请输入加密密钥 (JASYPT_ENCRYPTOR_PASSWORD): ");
            secret = scanner.nextLine();
            System.out.print("请输入要加密的明文密码: ");
            plain = scanner.nextLine();
        }

        if (secret.isEmpty() || plain.isEmpty()) {
            System.out.println("错误: 密钥和密码不能为空");
            return;
        }

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPassword(secret);
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
        encryptor.setPoolSize(1);

        String encrypted = encryptor.encrypt(plain);

        System.out.println("");
        System.out.println("========================================");
        System.out.println("加密结果:");
        System.out.println("========================================");
        System.out.println("ENC(" + encrypted + ")");
        System.out.println("");
        System.out.println("请将以上结果填入 application-prod.yml 的 password 字段");
        System.out.println("并在服务器上设置环境变量:");
        System.out.println("  export JASYPT_ENCRYPTOR_PASSWORD=" + secret);
        System.out.println("========================================");
    }
}
