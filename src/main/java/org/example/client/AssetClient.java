package org.example.client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import org.example.contract.Asset;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple1;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class AssetClient {

    static Logger logger = LoggerFactory.getLogger(AssetClient.class);

    private BcosSDK bcosSDK;
    private Client client;
    private CryptoKeyPair cryptoKeyPair;

    public void initialize() {
        @SuppressWarnings("resource")
        ApplicationContext context =
                new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
        bcosSDK = context.getBean(BcosSDK.class);
        client = bcosSDK.getClient();
        cryptoKeyPair = client.getCryptoSuite().getCryptoKeyPair();
        client.getCryptoSuite().setCryptoKeyPair(cryptoKeyPair);
        logger.debug("create client for group, account address is {}", cryptoKeyPair.getAddress());
    }


    public String loadAssetAddr() throws Exception {
        // load Asset contact address from contract.properties
        Properties prop = new Properties();
        final Resource contractResource = new ClassPathResource("contract.properties");
        prop.load(contractResource.getInputStream());

        String contractAddress = prop.getProperty("address");
        if (contractAddress == null || contractAddress.trim().equals("")) {
            throw new Exception(" load Asset contract address failed, please deploy it first. ");
        }
        logger.info(" load Asset address from contract.properties, address is {}", contractAddress);
        return contractAddress;
    }

    public void queryAssetAmount(String assetAccount) {
        try {
            String contractAddress = loadAssetAddr();
            Asset asset = Asset.load(contractAddress, client, cryptoKeyPair);
            Tuple2<Boolean, BigInteger> result = asset.select(assetAccount);
            if (result.getValue1()) {
                System.out.printf(" asset account %s, value %s %n", assetAccount, result.getValue2());
            } else {
                System.out.printf(" %s asset account is not exist %n", assetAccount);
            }
        } catch (Exception e) {
            logger.error(" queryAssetAmount exception, error message is {}", e.getMessage());

            System.out.printf(" query asset account failed, error message is %s%n", e.getMessage());
        }
    }

    public void registerAssetAccount(String assetAccount, BigInteger amount) {
        try {
            String contractAddress = loadAssetAddr();

            Asset asset = Asset.load(contractAddress, client, cryptoKeyPair);
            TransactionReceipt receipt = asset.register(assetAccount, amount);
            List<Asset.RegisterEventEventResponse> registerEventEvents = asset.getRegisterEventEvents(receipt);
            if (!registerEventEvents.isEmpty()) {
                if (registerEventEvents.get(0).ret.compareTo(BigInteger.ZERO) == 0) {
                    System.out.printf(
                            " register asset account success => asset: %s, value: %s %n", assetAccount, amount);
                } else {
                    System.out.printf(
                            " register asset account failed, ret code is %s %n", registerEventEvents.get(0).ret.toString());
                }
            } else {
                System.out.println(" event log not found, maybe transaction not exec, receipt status is: " + receipt.getStatus());
            }
        } catch (Exception e) {
            logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
            System.out.printf(" register asset account failed, error message is %s%n", e.getMessage());
        }
    }

    public void transferAsset(String fromAssetAccount, String toAssetAccount, BigInteger amount) {
        try {
            String contractAddress = loadAssetAddr();
            Asset asset = Asset.load(contractAddress, client, cryptoKeyPair);
            TransactionReceipt receipt = asset.transfer(fromAssetAccount, toAssetAccount, amount);
            List<Asset.TransferEventEventResponse> transferEventEvents = asset.getTransferEventEvents(receipt);
            if (!transferEventEvents.isEmpty()) {
                if (transferEventEvents.get(0).ret.compareTo(BigInteger.ZERO) == 0) {
                    System.out.printf(
                            " transfer success => from_asset: %s, to_asset: %s, amount: %s %n",
                            fromAssetAccount, toAssetAccount, amount);
                } else {
                    System.out.printf(
                            " transfer asset account failed, ret code is %s %n", transferEventEvents.get(0).ret.toString());
                }
            } else {
                System.out.println(" event log not found, maybe transaction not exec, status is: " + receipt.getStatus());
            }
        } catch (Exception e) {

            logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
            System.out.printf(" register asset account failed, error message is %s%n", e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {

        AssetClient client = new AssetClient();
        client.initialize();


        //Note: 记得将Alice2 Bob2换成新的名字，否则链上重复
        client.registerAssetAccount("Alice2", new BigInteger("10000"));
        client.registerAssetAccount("Bob2", new BigInteger("10000"));
        client.queryAssetAmount("Alice2");
        client.queryAssetAmount("Bob2");
        client.transferAsset("Alice2", "Bob2", new BigInteger("1000"));
        client.queryAssetAmount("Alice2");
        client.queryAssetAmount("Bob2");

        System.exit(0);
    }
}
