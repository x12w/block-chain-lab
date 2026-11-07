package com.wetech.demo.web3j.contracts.escrowenhanced;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.14.0.
 */
@SuppressWarnings("rawtypes")
public class EscrowEnhanced extends Contract {
    public static final String BINARY = "60c060405234801561000f575f5ffd5b50604051611a50380380611a5083398101604081905261002e91610132565b6001600160a01b0382161580159061004e57506001600160a01b03811615155b61009f5760405162461bcd60e51b815260206004820152601460248201527f457363726f773a207a65726f206172626974657200000000000000000000000060448201526064015b60405180910390fd5b806001600160a01b0316826001600160a01b0316036101005760405162461bcd60e51b815260206004820152601c60248201527f457363726f773a206172626974657273206d75737420646966666572000000006044820152606401610096565b6001600160a01b039182166080521660a052610163565b80516001600160a01b038116811461012d575f5ffd5b919050565b5f5f60408385031215610143575f5ffd5b61014c83610117565b915061015a60208401610117565b90509250929050565b60805160a0516118a26101ae5f395f8181610117015281816107cd0152818161097301526113b301525f81816101dc0152818161079b01528181610927015261136501526118a25ff3fe608060405234801561000f575f5ffd5b50600436106100cb575f3560e01c806386d6282c11610088578063d09ef24111610063578063d09ef241146101a4578063eed90d6b146101c4578063fe25e00a146101d7578063ff981099146101fe575f5ffd5b806386d6282c1461016b578063b6b55f251461017e578063ba0179b514610191575f5ffd5b806302bd3421146100cf5780632453ffa8146100f5578063278ecde1146100fd5780634e3dbe431461011257806352a34b051461015157806354fd4d5014610164575b5f5ffd5b6100e26100dd366004611423565b61023b565b6040519081526020015b60405180910390f35b6100e25f5481565b61011061010b3660046114c4565b61054d565b005b6101397f000000000000000000000000000000000000000000000000000000000000000081565b6040516001600160a01b0390911681526020016100ec565b61011061015f3660046114eb565b610728565b60026100e2565b6101106101793660046114c4565b610b3e565b61011061018c3660046114c4565b610c80565b61011061019f3660046114c4565b610e8c565b6101b76101b23660046114c4565b611028565b6040516100ec919061157b565b6101106101d23660046114c4565b6111ae565b6101397f000000000000000000000000000000000000000000000000000000000000000081565b61021161020c3660046114c4565b611321565b604080519415158552921515602085015290151591830191909152151560608201526080016100ec565b5f6001600160a01b0387161580159061025d57506001600160a01b0387163314155b6102a75760405162461bcd60e51b815260206004820152601660248201527522b9b1b937bb9d1034b73b30b634b21039b2b63632b960511b60448201526064015b60405180910390fd5b5f866001600160a01b03163b116103005760405162461bcd60e51b815260206004820152601f60248201527f457363726f773a20746f6b656e206973206e6f74206120636f6e747261637400604482015260640161029e565b5f85116103485760405162461bcd60e51b8152602060048201526016602482015275457363726f773a20616d6f756e74206973207a65726f60501b604482015260640161029e565b4284116103975760405162461bcd60e51b815260206004820152601c60248201527f457363726f773a20646561646c696e6520696e20746865207061737400000000604482015260640161029e565b5f805490806103a5836115fb565b9190505590506040518060e00160405280336001600160a01b03168152602001886001600160a01b03168152602001876001600160a01b031681526020018681526020018581526020015f600581111561040157610401611519565b815260200184848080601f0160208091040260200160405190810160405280939291908181526020018383808284375f92018290525093909452505083815260016020818152604092839020855181546001600160a01b03199081166001600160a01b039283161783559287015182850180548516918316919091179055938601516002820180549093169416939093179055606084015160038301556080840151600483015560a0840151600580840180549495509193919260ff19909216919084908111156104d4576104d4611519565b021790555060c082015160068201906104ed90826116bc565b5050604080516001600160a01b038981168252602082018990529181018790529089169150339083907fa4be1e061958d160de04db54668a64922c36d2765b16644ae91acf127708970e9060600160405180910390a49695505050505050565b805f54811061056e5760405162461bcd60e51b815260040161029e90611777565b5f8281526001602052604090205482906001600160a01b031633146105a55760405162461bcd60e51b815260040161029e906117ae565b826001805f83815260016020526040902060059081015460ff16908111156105cf576105cf611519565b146105ec5760405162461bcd60e51b815260040161029e906117e5565b5f8581526001602052604090206004810154421161064c5760405162461bcd60e51b815260206004820152601c60248201527f457363726f773a20646561646c696e65206e6f74207265616368656400000000604482015260640161029e565b60058101805460ff19166003908117909155600282015482549183015460405163a9059cbb60e01b81526001600160a01b039384166004820152602481019190915291169063a9059cbb906044016020604051808303815f875af11580156106b6573d5f5f3e3d5ffd5b505050506040513d601f19601f820116820180604052508101906106da9190611813565b6106f65760405162461bcd60e51b815260040161029e90611835565b60405186907f303a31648d19f17852cf82657162c4bdd4a06ab3f36f1e7ae1118f4ca6473e11905f90a2505050505050565b815f5481106107495760405162461bcd60e51b815260040161029e90611777565b826004805f83815260016020526040902060059081015460ff169081111561077357610773611519565b146107905760405162461bcd60e51b815260040161029e906117e5565b336001600160a01b037f00000000000000000000000000000000000000000000000000000000000000001614806107ef5750336001600160a01b037f000000000000000000000000000000000000000000000000000000000000000016145b61083b5760405162461bcd60e51b815260206004820181905260248201527f457363726f773a2063616c6c6572206973206e6f7420616e2061726269746572604482015260640161029e565b5f8581526002602090815260408083203384529091529020805460ff1615806108725750805460ff61010090910416151585151514155b6108b75760405162461bcd60e51b8152602060048201526016602482015275457363726f773a206475706c696361746520766f746560501b604482015260640161029e565b80548515156101000261ffff19909116176001178155604051339087907f73bb411c27e6a5f5f63204cf0bf3013ddf33f08d73f1a4a497e47e10557dd9519061090590891515815260200190565b60405180910390a35f8681526002602090815260408083206001600160a01b037f00000000000000000000000000000000000000000000000000000000000000008116855281845282852083518085018552905460ff808216151583526101009182900481161515838801527f0000000000000000000000000000000000000000000000000000000000000000909316875292855294839020835180850190945254808216151584529190910416151591810191909152815115806109c957508051155b806109e05750806020015115158260200151151514155b156109ed57505050610b37565b5f88815260016020526040902087610a06576003610a09565b60025b816005015f6101000a81548160ff02191690836005811115610a2d57610a2d611519565b021790555060028101546001600160a01b031663a9059cbb89610a5a5782546001600160a01b0316610a69565b60018301546001600160a01b03165b60038401546040516001600160e01b031960e085901b1681526001600160a01b03909216600483015260248201526044016020604051808303815f875af1158015610ab6573d5f5f3e3d5ffd5b505050506040513d601f19601f82011682018060405250810190610ada9190611813565b610af65760405162461bcd60e51b815260040161029e90611835565b887f649ff37700d4eb8a5bc86ef06a55183597fa5c3c6b53ec4b9ee510cb2089ee2689604051610b2a911515815260200190565b60405180910390a2505050505b5050505050565b805f548110610b5f5760405162461bcd60e51b815260040161029e90611777565b5f8281526001602081905260409091209060058083015460ff1690811115610b8957610b89611519565b1480610bab575060058181015460ff1681811115610ba957610ba9611519565b145b610bc75760405162461bcd60e51b815260040161029e906117e5565b80546001600160a01b0316331480610beb575060018101546001600160a01b031633145b610c375760405162461bcd60e51b815260206004820152601d60248201527f457363726f773a2063616c6c6572206973206e6f742061207061727479000000604482015260640161029e565b60058101805460ff1916600417905560405133815283907fde481be1936299e9233634e9f3cd92028f797634de15defa790fb2d7a013c95a9060200160405180910390a2505050565b805f548110610ca15760405162461bcd60e51b815260040161029e90611777565b5f8281526001602052604090205482906001600160a01b03163314610cd85760405162461bcd60e51b815260040161029e906117ae565b825f805f83815260016020526040902060059081015460ff1690811115610d0157610d01611519565b14610d1e5760405162461bcd60e51b815260040161029e906117e5565b5f8581526001602052604090206004810154421115610d7f5760405162461bcd60e51b815260206004820152601760248201527f457363726f773a20646561646c696e6520706173736564000000000000000000604482015260640161029e565b60058101805460ff19166001179055600281015460038201546040516323b872dd60e01b815233600482015230602482015260448101919091526001600160a01b03909116906323b872dd906064016020604051808303815f875af1158015610dea573d5f5f3e3d5ffd5b505050506040513d601f19601f82011682018060405250810190610e0e9190611813565b610e5a5760405162461bcd60e51b815260206004820152601b60248201527f457363726f773a207472616e7366657246726f6d206661696c65640000000000604482015260640161029e565b60405186907f15e33efda35e5da2fb096ba49585307ba468af144aea691da964aaff1a7f3a57905f90a2505050505050565b805f548110610ead5760405162461bcd60e51b815260040161029e90611777565b5f8281526001602052604090205482906001600160a01b03163314610ee45760405162461bcd60e51b815260040161029e906117ae565b5f8381526001602081905260409091209060058083015460ff1690811115610f0e57610f0e611519565b1480610f30575060058181015460ff1681811115610f2e57610f2e611519565b145b610f4c5760405162461bcd60e51b815260040161029e906117e5565b60058101805460ff191660029081179091558101546001820154600383015460405163a9059cbb60e01b81526001600160a01b039283166004820152602481019190915291169063a9059cbb906044016020604051808303815f875af1158015610fb8573d5f5f3e3d5ffd5b505050506040513d601f19601f82011682018060405250810190610fdc9190611813565b610ff85760405162461bcd60e51b815260040161029e90611835565b60405184907f02a7d4d9472af7118644884b9a0ee443540c0027e938dd0aa35be8ecbe946c0a905f90a250505050565b6110676040805160e0810182525f8082526020820181905291810182905260608101829052608081018290529060a08201908152602001606081525090565b815f5481106110885760405162461bcd60e51b815260040161029e90611777565b5f83815260016020818152604092839020835160e08101855281546001600160a01b03908116825293820154841692810192909252600281015490921692810192909252600381015460608301526004810154608083015260058082015460a084019160ff9091169081111561110057611100611519565b600581111561111157611111611519565b815260200160068201805461112590611633565b80601f016020809104026020016040519081016040528092919081815260200182805461115190611633565b801561119c5780601f106111735761010080835404028352916020019161119c565b820191905f5260205f20905b81548152906001019060200180831161117f57829003601f168201915b50505050508152505091505b50919050565b805f5481106111cf5760405162461bcd60e51b815260040161029e90611777565b816001805f83815260016020526040902060059081015460ff16908111156111f9576111f9611519565b146112165760405162461bcd60e51b815260040161029e906117e5565b5f848152600160208190526040909120908101546001600160a01b031633146112815760405162461bcd60e51b815260206004820181905260248201527f457363726f773a2063616c6c6572206973206e6f74207468652073656c6c6572604482015260640161029e565b80600401544211156112d55760405162461bcd60e51b815260206004820181905260248201527f457363726f773a20736869706d656e7420646561646c696e6520706173736564604482015260640161029e565b6005818101805460ff1916909117905560405133815285907f1a66e0c7d5d7422838f4f916462f11609026cbe8fb5d43ec23c45d04ddfcfca99060200160405180910390a25050505050565b5f5f5f5f845f5481106113465760405162461bcd60e51b815260040161029e90611777565b5050505f92835250506002602090815260408083206001600160a01b037f00000000000000000000000000000000000000000000000000000000000000008116855281845282852083518085018552905460ff8082161515835261010091829004811615158388019081527f000000000000000000000000000000000000000000000000000000000000000090941688529386529584902084518086019095525480841615158086529690049092161515929093018290525191519193919291565b80356001600160a01b038116811461141e575f5ffd5b919050565b5f5f5f5f5f5f60a08789031215611438575f5ffd5b61144187611408565b955061144f60208801611408565b94506040870135935060608701359250608087013567ffffffffffffffff811115611478575f5ffd5b8701601f81018913611488575f5ffd5b803567ffffffffffffffff81111561149e575f5ffd5b8960208284010111156114af575f5ffd5b60208201935080925050509295509295509295565b5f602082840312156114d4575f5ffd5b5035919050565b80151581146114e8575f5ffd5b50565b5f5f604083850312156114fc575f5ffd5b82359150602083013561150e816114db565b809150509250929050565b634e487b7160e01b5f52602160045260245ffd5b6006811061154957634e487b7160e01b5f52602160045260245ffd5b9052565b5f81518084528060208401602086015e5f602082860101526020601f19601f83011685010191505092915050565b6020815260018060a01b03825116602082015260018060a01b03602083015116604082015260018060a01b03604083015116606082015260608201516080820152608082015160a08201525f60a08301516115d960c084018261152d565b5060c083015160e0808401526115f361010084018261154d565b949350505050565b5f6001820161161857634e487b7160e01b5f52601160045260245ffd5b5060010190565b634e487b7160e01b5f52604160045260245ffd5b600181811c9082168061164757607f821691505b6020821081036111a857634e487b7160e01b5f52602260045260245ffd5b601f8211156116b757828211156116b757805f5260205f20601f840160051c602085101561169057505f5b90810190601f840160051c035f5b818110156116b3575f8382015560010161169e565b5050505b505050565b815167ffffffffffffffff8111156116d6576116d661161f565b6116ea816116e48454611633565b84611665565b6020601f82116001811461171c575f83156117055750848201515b5f19600385901b1c1916600184901b178455610b37565b5f84815260208120601f198516915b8281101561174b578785015182556020948501946001909201910161172b565b508482101561176857868401515f19600387901b60f8161c191681555b50505050600190811b01905550565b6020808252601c908201527f457363726f773a206f7264657220646f6573206e6f7420657869737400000000604082015260600190565b6020808252601f908201527f457363726f773a2063616c6c6572206973206e6f742074686520627579657200604082015260600190565b602080825260149082015273457363726f773a2077726f6e672073746174757360601b604082015260600190565b5f60208284031215611823575f5ffd5b815161182e816114db565b9392505050565b60208082526017908201527f457363726f773a207472616e73666572206661696c656400000000000000000060408201526060019056fea2646970667358221220311bbeca32529e0d1837320adfd6c099433a54cb45ff9f886de8eba685936f5b64736f6c63430008230033";

    private static String librariesLinkedBinary;

    public static final String FUNC_ARBITER = "arbiter";

    public static final String FUNC_CONFIRM = "confirm";

    public static final String FUNC_CREATEORDER = "createOrder";

    public static final String FUNC_DEPOSIT = "deposit";

    public static final String FUNC_DISPUTE = "dispute";

    public static final String FUNC_GETORDER = "getOrder";

    public static final String FUNC_GETVOTES = "getVotes";

    public static final String FUNC_MARKSHIPPED = "markShipped";

    public static final String FUNC_ORDERCOUNT = "orderCount";

    public static final String FUNC_REFUND = "refund";

    public static final String FUNC_RESOLVE = "resolve";

    public static final String FUNC_SECONDARBITER = "secondArbiter";

    public static final String FUNC_VERSION = "version";

    public static final Event ARBITRATIONVOTED_EVENT = new Event("ArbitrationVoted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Bool>() {}));
    ;

    public static final Event ORDERCOMPLETED_EVENT = new Event("OrderCompleted", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event ORDERCREATED_EVENT = new Event("OrderCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event ORDERDISPUTED_EVENT = new Event("OrderDisputed", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>() {}));
    ;

    public static final Event ORDERPAID_EVENT = new Event("OrderPaid", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event ORDERREFUNDED_EVENT = new Event("OrderRefunded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}));
    ;

    public static final Event ORDERRESOLVED_EVENT = new Event("OrderResolved", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Bool>() {}));
    ;

    public static final Event ORDERSHIPPED_EVENT = new Event("OrderShipped", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>(true) {}, new TypeReference<Address>() {}));
    ;

    @Deprecated
    protected EscrowEnhanced(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected EscrowEnhanced(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected EscrowEnhanced(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected EscrowEnhanced(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<ArbitrationVotedEventResponse> getArbitrationVotedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ARBITRATIONVOTED_EVENT, transactionReceipt);
        ArrayList<ArbitrationVotedEventResponse> responses = new ArrayList<ArbitrationVotedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ArbitrationVotedEventResponse typedResponse = new ArbitrationVotedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.voter = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.sellerWins = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static ArbitrationVotedEventResponse getArbitrationVotedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ARBITRATIONVOTED_EVENT, log);
        ArbitrationVotedEventResponse typedResponse = new ArbitrationVotedEventResponse();
        typedResponse.log = log;
        typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.voter = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.sellerWins = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ArbitrationVotedEventResponse> arbitrationVotedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getArbitrationVotedEventFromLog(log));
    }

    public Flowable<ArbitrationVotedEventResponse> arbitrationVotedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ARBITRATIONVOTED_EVENT));
        return arbitrationVotedEventFlowable(filter);
    }

    public static List<OrderCompletedEventResponse> getOrderCompletedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERCOMPLETED_EVENT, transactionReceipt);
        ArrayList<OrderCompletedEventResponse> responses = new ArrayList<OrderCompletedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderCompletedEventResponse typedResponse = new OrderCompletedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderCompletedEventResponse getOrderCompletedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERCOMPLETED_EVENT, log);
        OrderCompletedEventResponse typedResponse = new OrderCompletedEventResponse();
        typedResponse.log = log;
        typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<OrderCompletedEventResponse> orderCompletedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderCompletedEventFromLog(log));
    }

    public Flowable<OrderCompletedEventResponse> orderCompletedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERCOMPLETED_EVENT));
        return orderCompletedEventFlowable(filter);
    }

    public static List<OrderCreatedEventResponse> getOrderCreatedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERCREATED_EVENT, transactionReceipt);
        ArrayList<OrderCreatedEventResponse> responses = new ArrayList<OrderCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderCreatedEventResponse typedResponse = new OrderCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.buyer = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.seller = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.token = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.deadline = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderCreatedEventResponse getOrderCreatedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERCREATED_EVENT, log);
        OrderCreatedEventResponse typedResponse = new OrderCreatedEventResponse();
        typedResponse.log = log;
        typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.buyer = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.seller = (String) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.token = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.amount = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.deadline = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<OrderCreatedEventResponse> orderCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderCreatedEventFromLog(log));
    }

    public Flowable<OrderCreatedEventResponse> orderCreatedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERCREATED_EVENT));
        return orderCreatedEventFlowable(filter);
    }

    public static List<OrderDisputedEventResponse> getOrderDisputedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERDISPUTED_EVENT, transactionReceipt);
        ArrayList<OrderDisputedEventResponse> responses = new ArrayList<OrderDisputedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderDisputedEventResponse typedResponse = new OrderDisputedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.by = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderDisputedEventResponse getOrderDisputedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERDISPUTED_EVENT, log);
        OrderDisputedEventResponse typedResponse = new OrderDisputedEventResponse();
        typedResponse.log = log;
        typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.by = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<OrderDisputedEventResponse> orderDisputedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderDisputedEventFromLog(log));
    }

    public Flowable<OrderDisputedEventResponse> orderDisputedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERDISPUTED_EVENT));
        return orderDisputedEventFlowable(filter);
    }

    public static List<OrderPaidEventResponse> getOrderPaidEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERPAID_EVENT, transactionReceipt);
        ArrayList<OrderPaidEventResponse> responses = new ArrayList<OrderPaidEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderPaidEventResponse typedResponse = new OrderPaidEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderPaidEventResponse getOrderPaidEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERPAID_EVENT, log);
        OrderPaidEventResponse typedResponse = new OrderPaidEventResponse();
        typedResponse.log = log;
        typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<OrderPaidEventResponse> orderPaidEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderPaidEventFromLog(log));
    }

    public Flowable<OrderPaidEventResponse> orderPaidEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERPAID_EVENT));
        return orderPaidEventFlowable(filter);
    }

    public static List<OrderRefundedEventResponse> getOrderRefundedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERREFUNDED_EVENT, transactionReceipt);
        ArrayList<OrderRefundedEventResponse> responses = new ArrayList<OrderRefundedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderRefundedEventResponse typedResponse = new OrderRefundedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderRefundedEventResponse getOrderRefundedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERREFUNDED_EVENT, log);
        OrderRefundedEventResponse typedResponse = new OrderRefundedEventResponse();
        typedResponse.log = log;
        typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<OrderRefundedEventResponse> orderRefundedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderRefundedEventFromLog(log));
    }

    public Flowable<OrderRefundedEventResponse> orderRefundedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERREFUNDED_EVENT));
        return orderRefundedEventFlowable(filter);
    }

    public static List<OrderResolvedEventResponse> getOrderResolvedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERRESOLVED_EVENT, transactionReceipt);
        ArrayList<OrderResolvedEventResponse> responses = new ArrayList<OrderResolvedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderResolvedEventResponse typedResponse = new OrderResolvedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.sellerWins = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderResolvedEventResponse getOrderResolvedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERRESOLVED_EVENT, log);
        OrderResolvedEventResponse typedResponse = new OrderResolvedEventResponse();
        typedResponse.log = log;
        typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.sellerWins = (Boolean) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<OrderResolvedEventResponse> orderResolvedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderResolvedEventFromLog(log));
    }

    public Flowable<OrderResolvedEventResponse> orderResolvedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERRESOLVED_EVENT));
        return orderResolvedEventFlowable(filter);
    }

    public static List<OrderShippedEventResponse> getOrderShippedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ORDERSHIPPED_EVENT, transactionReceipt);
        ArrayList<OrderShippedEventResponse> responses = new ArrayList<OrderShippedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OrderShippedEventResponse typedResponse = new OrderShippedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.seller = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static OrderShippedEventResponse getOrderShippedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ORDERSHIPPED_EVENT, log);
        OrderShippedEventResponse typedResponse = new OrderShippedEventResponse();
        typedResponse.log = log;
        typedResponse.id = (BigInteger) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.seller = (String) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<OrderShippedEventResponse> orderShippedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getOrderShippedEventFromLog(log));
    }

    public Flowable<OrderShippedEventResponse> orderShippedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ORDERSHIPPED_EVENT));
        return orderShippedEventFlowable(filter);
    }

    public RemoteFunctionCall<String> arbiter() {
        final Function function = new Function(FUNC_ARBITER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> confirm(BigInteger id) {
        final Function function = new Function(
                FUNC_CONFIRM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(id)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> createOrder(String seller, String token,
            BigInteger amount, BigInteger deadline, String description) {
        final Function function = new Function(
                FUNC_CREATEORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, seller), 
                new org.web3j.abi.datatypes.Address(160, token), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.generated.Uint256(deadline), 
                new org.web3j.abi.datatypes.Utf8String(description)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> deposit(BigInteger id) {
        final Function function = new Function(
                FUNC_DEPOSIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(id)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> dispute(BigInteger id) {
        final Function function = new Function(
                FUNC_DISPUTE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(id)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Order> getOrder(BigInteger id) {
        final Function function = new Function(FUNC_GETORDER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(id)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Order>() {}));
        return executeRemoteCallSingleValueReturn(function, Order.class);
    }

    public RemoteFunctionCall<Tuple4<Boolean, Boolean, Boolean, Boolean>> getVotes(BigInteger id) {
        final Function function = new Function(FUNC_GETVOTES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(id)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}, new TypeReference<Bool>() {}, new TypeReference<Bool>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple4<Boolean, Boolean, Boolean, Boolean>>(function,
                new Callable<Tuple4<Boolean, Boolean, Boolean, Boolean>>() {
                    @Override
                    public Tuple4<Boolean, Boolean, Boolean, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<Boolean, Boolean, Boolean, Boolean>(
                                (Boolean) results.get(0).getValue(), 
                                (Boolean) results.get(1).getValue(), 
                                (Boolean) results.get(2).getValue(), 
                                (Boolean) results.get(3).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> markShipped(BigInteger id) {
        final Function function = new Function(
                FUNC_MARKSHIPPED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(id)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> orderCount() {
        final Function function = new Function(FUNC_ORDERCOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> refund(BigInteger id) {
        final Function function = new Function(
                FUNC_REFUND, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(id)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> resolve(BigInteger id, Boolean sellerWins) {
        final Function function = new Function(
                FUNC_RESOLVE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(id), 
                new org.web3j.abi.datatypes.Bool(sellerWins)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> secondArbiter() {
        final Function function = new Function(FUNC_SECONDARBITER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> version() {
        final Function function = new Function(FUNC_VERSION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    @Deprecated
    public static EscrowEnhanced load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new EscrowEnhanced(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static EscrowEnhanced load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new EscrowEnhanced(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static EscrowEnhanced load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new EscrowEnhanced(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static EscrowEnhanced load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new EscrowEnhanced(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<EscrowEnhanced> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider, String first, String second) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, first), 
                new org.web3j.abi.datatypes.Address(160, second)));
        return deployRemoteCall(EscrowEnhanced.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<EscrowEnhanced> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider,
            String first, String second) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, first), 
                new org.web3j.abi.datatypes.Address(160, second)));
        return deployRemoteCall(EscrowEnhanced.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<EscrowEnhanced> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit, String first, String second) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, first), 
                new org.web3j.abi.datatypes.Address(160, second)));
        return deployRemoteCall(EscrowEnhanced.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<EscrowEnhanced> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit,
            String first, String second) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, first), 
                new org.web3j.abi.datatypes.Address(160, second)));
        return deployRemoteCall(EscrowEnhanced.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class Order extends DynamicStruct {
        public String buyer;

        public String seller;

        public String token;

        public BigInteger amount;

        public BigInteger deadline;

        public BigInteger status;

        public String description;

        public Order(String buyer, String seller, String token, BigInteger amount,
                BigInteger deadline, BigInteger status, String description) {
            super(new org.web3j.abi.datatypes.Address(160, buyer), 
                    new org.web3j.abi.datatypes.Address(160, seller), 
                    new org.web3j.abi.datatypes.Address(160, token), 
                    new org.web3j.abi.datatypes.generated.Uint256(amount), 
                    new org.web3j.abi.datatypes.generated.Uint256(deadline), 
                    new org.web3j.abi.datatypes.generated.Uint8(status), 
                    new org.web3j.abi.datatypes.Utf8String(description));
            this.buyer = buyer;
            this.seller = seller;
            this.token = token;
            this.amount = amount;
            this.deadline = deadline;
            this.status = status;
            this.description = description;
        }

        public Order(Address buyer, Address seller, Address token, Uint256 amount, Uint256 deadline,
                Uint8 status, Utf8String description) {
            super(buyer, seller, token, amount, deadline, status, description);
            this.buyer = buyer.getValue();
            this.seller = seller.getValue();
            this.token = token.getValue();
            this.amount = amount.getValue();
            this.deadline = deadline.getValue();
            this.status = status.getValue();
            this.description = description.getValue();
        }
    }

    public static class ArbitrationVotedEventResponse extends BaseEventResponse {
        public BigInteger id;

        public String voter;

        public Boolean sellerWins;
    }

    public static class OrderCompletedEventResponse extends BaseEventResponse {
        public BigInteger id;
    }

    public static class OrderCreatedEventResponse extends BaseEventResponse {
        public BigInteger id;

        public String buyer;

        public String seller;

        public String token;

        public BigInteger amount;

        public BigInteger deadline;
    }

    public static class OrderDisputedEventResponse extends BaseEventResponse {
        public BigInteger id;

        public String by;
    }

    public static class OrderPaidEventResponse extends BaseEventResponse {
        public BigInteger id;
    }

    public static class OrderRefundedEventResponse extends BaseEventResponse {
        public BigInteger id;
    }

    public static class OrderResolvedEventResponse extends BaseEventResponse {
        public BigInteger id;

        public Boolean sellerWins;
    }

    public static class OrderShippedEventResponse extends BaseEventResponse {
        public BigInteger id;

        public String seller;
    }
}
