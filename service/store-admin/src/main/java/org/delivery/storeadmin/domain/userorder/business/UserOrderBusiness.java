package org.delivery.storeadmin.domain.userorder.business;

import lombok.RequiredArgsConstructor;
import org.delivery.common.message.model.UserOrderMessage;
import org.delivery.storeadmin.domain.sse.connection.SseConnectionPool;
import org.delivery.storeadmin.domain.storemenu.converter.StoreMenuConverter;
import org.delivery.storeadmin.domain.storemenu.service.StoreMenuService;
import org.delivery.storeadmin.domain.userorder.controller.model.UserOrderDetailResponse;
import org.delivery.storeadmin.domain.userorder.converter.UserOrderConverter;
import org.delivery.storeadmin.domain.userorder.service.UserOrderService;
import org.delivery.storeadmin.domain.userordermenu.service.UserOrderMenuService;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserOrderBusiness {

    private final UserOrderService userOrderService;

    private final SseConnectionPool sseConnectionPool;

    private final UserOrderMenuService userOrderMenuService;

    private final StoreMenuService storeMenuService;

    private final StoreMenuConverter storeMenuConverter;

    private final UserOrderConverter userOrderConverter;


    public void pushUserOder(UserOrderMessage userOrderMessage){
        var userOrderEntity = userOrderService.getUserOrder(userOrderMessage.getUserOrderId()).orElseThrow(
                ()-> new RuntimeException("사용자 주문내역 없음"));

        var userOrderMenuList = userOrderMenuService.getUserOrderMenuList(userOrderEntity.getUserId());

        var storeMenuResponseList = userOrderMenuList.stream()
                .map(it -> {
                    return storeMenuService.getStoreMenuWithThrow(it.getStoreMenuId());
                })
                .map(it ->{
                    return storeMenuConverter.toResponse(it);
                })
                .collect(Collectors.toList());

        var userOrderResponse = userOrderConverter.toResponse(userOrderEntity);

        var push = UserOrderDetailResponse.builder()
                .userOrderResponse(userOrderResponse)
                .storeMenuResponseList(storeMenuResponseList)
                .build();

        var userConnection = sseConnectionPool.getSession(userOrderEntity.getStoreId().toString());


        userConnection.sendMessage(push);

    }

}
