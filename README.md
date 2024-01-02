# JAVA SDK Demo for Dabashou


### 测试链信息

- 节点数：4
- rpc addr: 
  - 18.163.74.253:20200 
  - 18.163.74.253:20201
  - 18.163.74.253:20202
  - 18.163.74.253:20203 

### 客户端信息

#### 软件依赖

- Java: JDK 1.8 - JDK 14均可
- 区块链交互工具: fisco-bcos-java-sdk:3.3.0
- 构建工具这里采用了gradle,版本为6.3，以供参考


#### java应用构建引导(以gradle项目为例)        
##### 前期配置
1. 创建java gradle项目（或引入本示例项目）
2. 引入依赖BCOS库,以 参考[这里](./build.gradle),（7.0以上版本gradle不兼容)
3. 创建配置文件，（或从本项目中直接拷贝）
   1. [applicationContext.xml](src/main/resources/applicationContext.xml)
        这里主要关注：
      1. cryptoMaterial模块，这里用于配置证书路径，默认是放在resource/conf目录下面
      2. network模块，配置节点信息，这里可以根据区块链部署信息进行配置（测试链信息已配置好）
   2. [contract.properties](src/main/resources/contract.properties)
      这里主要配置链上合约的地址（可以理解为链上程序的ID），会在开发时由链上提供并配置。
      当前demo只有一个合约，真实生产环境将会有多个合约。
4. 配置证书
    客户端与节点通信需要由证书参与，当区块链节点部署完成后，可以得到对应的证书文件，并且拷贝到相应目录下。
    此处按照默认配置会放在resource/conf目录下面，一共五个文件。
5. 拷贝智能合约JAVA文件
    可以理解为合约的JAVA适配器，将合约接口封装成了JAVA接口用于直接调用，当区块链上逻辑开发测试完成后，会得到该部分文件。而在未来链上逻辑更新或是新增模块时，需要引入新文件。
    这部分文件当前位于org.example.contract包下。
6. 至此，前期配置部分完成。

##### 开发客户端业务 (以一个简单的链上账本为例)
1. 需求是在区块链上维护一个账本，里面记录了每个账户的余额，并且可以进行简单的转账操作。出于简单考虑，不涉及权限管理，不涉及异步调用。
2. 此时区块链已经完成搭建并且链上逻辑已经开发完成。
3. 链上账本提供三个简单接口，**登记**，**查询**，**转账**，并封装成智能合约JAVA文件（即[Asset.java](src/main/java/org/example/contract/Asset.java)）：
   1. 登记：``` TransactionReceipt register(String account, BigInteger asset_value)``` 即向账本登记某人拥有的金额，例如想要登记Alice拥有10000元 ```register("Alice", new BigInteger("10000")) ```
   2. 查询：``` Tuple2<Boolean, BigInteger> select(String account)```  查询某一账户余额，返回参数1为True时，参数2为目标账户余额。
   3. 转账：``` TransactionReceipt transfer(String from_account, String to_account, BigInteger amount)``` 
4. 现在开始进行客户端逻辑开发，代码都在[AssetClient.java](src/main/java/org/example/client/AssetClient.java)
5. 首先需要初始化一些对象，主要包括SDK client和秘钥对CryptoKeyPair。其中client用于链上交互，CryptoKeyPair负责保存调用者身份信息。
  ``` 
        ApplicationContext context =new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
        bcosSDK = context.getBean(BcosSDK.class);
        client = bcosSDK.getClient();
        cryptoKeyPair = client.getCryptoSuite().getCryptoKeyPair();
        client.getCryptoSuite().setCryptoKeyPair(cryptoKeyPair);
```

6. 然后在与合约交互之前，需要构造合约类对象，参数分别为 (1)合约地址（参考上文描述，存在[contract.properties](src/main/resources/contract.properties)，读出来就行） (2) SDK client对象， （3）秘钥对cryptoKeyPair对象

   ``` Asset asset = Asset.load(contractAddress, client, cryptoKeyPair); ```
7. 然后就可以直接调用上述接口了。需要注意的是，
   1. 读取类的接口可以立即拿到返回值，例如查询，具体可以参考
    ```queryAssetAmount(String assetAccount)```这个方法中的写法。 
   2. 而写入类的接口，即需要向链上更新信息的，一般不设定有具体的返回值，也不会直接通过代码获取返回值。而是得到TransactionReceipt，通过解析TransactionReceipt来获取调用状态，例如是否转账成功。
     具体可以参考```transferAsset(String fromAssetAccount, String toAssetAccount, BigInteger amount) ```方法。

8. 至此一个简单的客户端就可以编写完成，参考[AssetClient.java](src/main/java/org/example/client/AssetClient.java)的实例即可。
