package com.OLP.books.mq;


import com.OLP.books.service.BookService;
import com.OLP.common.entity.MqConstants;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BookListener {

        private final BookService bookService;

        /**
         * 监听书籍新增或修改的业务
         * @param id
         */
        @RabbitListener(queues = MqConstants.BOOKS_INSERTORUPDATE_QUEUE)
        public void listenBooksInsertOrUpdate(Long id){
                bookService.insertOrUpdateById(id);

        }


        /**
         * 监听书籍删除的业务
         * @param id
         */
        @RabbitListener(queues = MqConstants.BOOKS_DELETE_QUEUE)
        public void listenBooksdelete(Long id){
                bookService.deleteDoc(id);
        }

        /**
         * 监听书籍点击量的业务
         * @param id
         */
        @RabbitListener(queues = MqConstants.BOOKS_CLICK_QUEUE)
        public void  listenBookClick(Long id) {
                bookService.UpdateclickDoc(id);
        }
}
