package com.example.escapadeserver3.Adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.escapadeserver3.Interface.ItemClickListener;
import com.example.escapadeserver3.Model.Order;
import com.example.escapadeserver3.R;
import com.example.escapadeserver3.Utils.Common;
import com.example.escapadeserver3.ViewOrderDetail;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderViewHolder> {

    Context context;
    List<Order> orderList;

    public OrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View itemview = LayoutInflater.from(context).inflate(R.layout.order_layout, viewGroup,false);
        return new OrderViewHolder(itemview);    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder orderViewHolder, final int i) {


        orderViewHolder.txt_order_id.setText(new StringBuilder("#").append(orderList.get(i).getOrderId()));
        orderViewHolder.txt_order_status.setText(new StringBuilder("Order Status: ").append(Common.convertCodeToStatus(orderList.get(i).getOrderStatus())));

        orderViewHolder.setItemClickListener(new ItemClickListener() {

            @Override
            public void Onclick(View view, Boolean isLongClick) {

                Common.currentOrder = orderList.get(i);

                context.startActivity(new Intent(context, ViewOrderDetail.class));

            }
        });

    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }
}
