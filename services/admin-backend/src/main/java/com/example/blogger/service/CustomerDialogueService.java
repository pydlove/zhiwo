package com.example.blogger.service;

import com.example.blogger.entity.CustomerDialogue;
import com.example.blogger.mapper.CustomerDialogueMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerDialogueService {

    private final CustomerDialogueMapper customerDialogueMapper;

    public CustomerDialogueService(CustomerDialogueMapper customerDialogueMapper) {
        this.customerDialogueMapper = customerDialogueMapper;
    }

    public List<CustomerDialogue> list() {
        return customerDialogueMapper.findAll();
    }

    public List<CustomerDialogue> listByCategory(String category) {
        return customerDialogueMapper.findByCategory(category);
    }

    public List<CustomerDialogue> listByAdminId(String adminId) {
        return customerDialogueMapper.findByAdminId(adminId);
    }

    public List<CustomerDialogue> listByCategoryAndAdminId(String category, String adminId) {
        return customerDialogueMapper.findByCategoryAndAdminId(category, adminId);
    }

    public List<String> listCategories() {
        return customerDialogueMapper.findAllCategories();
    }

    public List<String> listCategoriesByAdminId(String adminId) {
        return customerDialogueMapper.findCategoriesByAdminId(adminId);
    }

    public List<CustomerDialogue> listByIds(List<String> ids) {
        return customerDialogueMapper.findByIds(ids);
    }

    public CustomerDialogue getById(String id) {
        return customerDialogueMapper.findById(id);
    }

    public void save(CustomerDialogue customerDialogue) {
        if (customerDialogue.getId() == null || customerDialogue.getId().isEmpty()) {
            customerDialogue.setId(UUID.randomUUID().toString().replace("-", ""));
            customerDialogueMapper.insert(customerDialogue);
        } else {
            customerDialogueMapper.update(customerDialogue);
        }
    }

    public void delete(String id) {
        customerDialogueMapper.delete(id);
    }
}
