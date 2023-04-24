package w.mazebank.services;

import w.mazebank.models.Transaction;
import w.mazebank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    public void transferMoney(Transaction transaction){
        transactionRepository.save(transaction);
    }
}
