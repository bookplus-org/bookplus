package com.bookplus.auth.domain.port.out;

import com.bookplus.auth.domain.model.User;

/** Puerto de salida — persistir usuario. */
public interface SaveUserPort {
    User save(User user);
}
