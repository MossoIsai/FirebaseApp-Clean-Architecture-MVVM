package ro.alexmamo.firebaseapp.main.products;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import static ro.alexmamo.firebaseapp.utils.Constants.ASCENDING;
import static ro.alexmamo.firebaseapp.utils.Constants.ESCAPE_CHARACTER;
import static ro.alexmamo.firebaseapp.utils.Constants.ITEMS_PER_PAGE;
import static ro.alexmamo.firebaseapp.utils.Constants.PRODUCT_NAME_PROPERTY;

@SuppressWarnings("ConstantConditions")
public class ProductDataSource extends PageKeyedDataSource<Integer, Product> {
    private String searchText;
    private Query initialQuery;
    private DocumentSnapshot lastVisible;
    private boolean lastPageReached;
    private int pageNumber = 1;

    ProductDataSource(String searchText, CollectionReference productsRef) {
        this.searchText = searchText;
        initialQuery = productsRef.orderBy(PRODUCT_NAME_PROPERTY, ASCENDING).limit(ITEMS_PER_PAGE);
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Product> callback) {
        if (searchText != null) {
            initialQuery = initialQuery.startAt(searchText).endAt(searchText + ESCAPE_CHARACTER);
        }
        initialQuery.get().addOnCompleteListener(task -> {
            List<Product> productList = new ArrayList<>();
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    Product product = document.toObject(Product.class);
                    productList.add(product);
                }
                callback.onResult(productList, null, pageNumber);
                int querySnapshotSize = querySnapshot.size() - 1;
                if (querySnapshotSize != -1) {
                    lastVisible = querySnapshot.getDocuments().get(querySnapshotSize);
                }
            }
        });
    }

    @Override
    public void loadBefore(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Product> callback) {}

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Product> callback) {
        Query nextQuery = initialQuery.startAfter(lastVisible);
        nextQuery.get().addOnCompleteListener(task -> {
            List<Product> productList = new ArrayList<>();
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if(!lastPageReached) {
                    for(QueryDocumentSnapshot document : querySnapshot){
                        productList.add(document.toObject(Product.class));
                    }
                    callback.onResult(productList, pageNumber);
                    pageNumber++;

                    if (productList.size() < ITEMS_PER_PAGE) {
                        lastPageReached = true;
                    } else {
                        lastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                    }
                }
            }
        });
    }
}