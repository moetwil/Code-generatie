package w.mazebank.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import w.mazebank.models.Transaction;
import w.mazebank.repository.TransactionRepository;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction transferMoney(Transaction transaction){
        transactionRepository.save(transaction);
        return transaction;
    }
}
