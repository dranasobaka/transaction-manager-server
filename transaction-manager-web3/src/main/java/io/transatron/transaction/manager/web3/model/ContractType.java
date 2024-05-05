package io.transatron.transaction.manager.web3.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ContractType {

    UndefinedType(-1),
    AccountCreateContract(0),
    TransferContract(1),
    TransferAssetContract(2),
    VoteAssetContract(3),
    VoteWitnessContract(4),
    WitnessCreateContract(5),
    AssetIssueContract(6),
    WitnessUpdateContract(8),
    ParticipateAssetIssueContract(9),
    AccountUpdateContract(10),
    FreezeBalanceContract(11),
    UnfreezeBalanceContract(12),
    WithdrawBalanceContract(13),
    UnfreezeAssetContract(14),
    UpdateAssetContract(15),
    ProposalCreateContract(16),
    ProposalApproveContract(17),
    ProposalDeleteContract(18),
    SetAccountIdContract(19),
    CustomContract(20),
    CreateSmartContract(30),
    TriggerSmartContract(31),
    GetContract(32),
    UpdateSettingContract(33),
    ExchangeCreateContract(41),
    ExchangeInjectContract(42),
    ExchangeWithdrawContract(43),
    ExchangeTransactionContract(44),
    UpdateEnergyLimitContract(45),
    AccountPermissionUpdateContract(46),
    ClearABIContract(48),
    UpdateBrokerageContract(49),
    ShieldedTransferContract(51),
    MarketSellAssetContract(52),
    MarketCancelOrderContract(53),
    FreezeBalanceV2Contract(54),
    UnfreezeBalanceV2Contract(55),
    WithdrawExpireUnfreezeContract(56),
    DelegateResourceContract(57),
    UnDelegateResourceContract(58),
    CancelAllUnfreezeV2Contract(59);

    private final int num;

    public static ContractType getContractTypeByNum(final int num) {
        return Arrays.stream(ContractType.values())
                     .filter(type -> type.getNum() == num)
                     .findFirst()
                     .orElse(ContractType.UndefinedType);
    }

}
