package org.example.service;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.bean.Appointment;

public interface AppointmentService extends IService<Appointment> {
    Appointment getOne(Appointment appointment);
}