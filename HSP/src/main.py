import numpy as np
from sklearn import preprocessing
'''
以下都是input
'''
dim = 2
num_elements = 6
data = np.array([[3, 1], [-2, -1], [-3, -2], [-1, -1], [2, 1], [3, 2]])
degree=[]#这里要得到知识图谱中每个实体的度（出入都包括,一般是同类型之间的度），作为input，自行计算
file_path=''

def preprocessing():
    # 归一化处理,使用的是z-scores
    # scaler = preprocessing.StandardScaler()
    # data_scaled = scaler.fit_transform(data)
    #归一化处理，使用的是min-max,(0,1)
    min_max_scaler = preprocessing.MinMaxScaler()
    data_scaled = min_max_scaler.fit_transform(data)
    return data_scaled

def get_r():
    '''
    归一化后计算平均距离和hyperparameter半棱长r
    :return: r
    '''
    data_scaled=preprocessing()
    #找平均距离和半棱长r
    J=[]#平均距离
    m, n = data_scaled.shape#array的行和列，n=num_elements，m=dim
    for i in range(n):
        data_column=data_scaled[:,i]
        sum=0
        for j in range(1,len(data_column)):
            sum+=data_column[j]-data_column[j-1]
        J[i]=sum/(num_elements-1)
    s=1
    for i in J:
        s=s * i
    r=pow(s,1/dim)#开根号得到r
    return r

def change_r():
    '''
    对r进行缩放
    :return:
    '''
    sum_d=0
    for i in range(num_elements):
        sum_d+=degree[i]
    #计算平均度d_mean和w
    d_mean=sum_d/num_elements
    w=d_mean/pow(2,dim)
    #缩放后的r_new
    r_new=pow(w,1/dim)*get_r()

    return r_new

def find_neighbor(data_scaled):
    '''
    得到hypercube中最近邻节点
    :param data_scaled:
    :return:
    '''
    cr=change_r()
    #得到每维特征排序后的data_array，方便后续查找数据，先排序，复杂度是O(dim*n*logn),
    data_sorted = np.sort(data_scaled, axis=0)  # 按列排序
    index_sorted = np.argsort(data_scaled, axis=0)  # 按列排序的到变化后的索引
    #排序之后顺序换了样本顺序换了，需要保存下来故有了index_sorted
    '''
    二分查找得到区间(f-r，f+r)的样本,只是要找到区间中的值，二分找而不是从前往后O(n)
    '''
    result=[]

    for i in range(num_elements):
        #求第i个样本点的近邻点
        res_array=[]
        for j in range(dim):
            min=data_scaled[i][j]-cr
            max=data_scaled[i][j]+cr
            temp_D = data_sorted[:,j]
            temp_I = index_sorted[:,j]
            res=BinarySearch_interval(temp_D,temp_I,min,max)
            res_array.append(res)
        #求交集intersection
        l=[]
        l.append(i)
        s=set()
        for re in res_array:
            s=s.intersection(set(re))
        result.append(l+list(s))

    return result


def BinarySearch_interval(list,list_Index,min,max):
    '''
    找出该维度特征上对应区间的（min,max）的点，返回这些点（点最原始的索引值）
    :param list:
    :param list_Index:
    :param min:
    :param max:
    :return:
    '''
    res=[]
    start=0
    end=len(list)
    while start<=end:
        mid=(start+end)/2
        if list[mid]>=min and list[mid]<=max:
            break
        if list[mid]<min:
            start=mid+1
        if list[mid]>max:
            end=mid-1

    i=mid
    j=mid
    while list[i]>=min and list[i]<=max:
        res.append(list_Index[i])
        i=i-1

    while list[j]>=min and list[j]<= max:
        res.append(list_Index[j])
        j=j+1
    return res

def write_txt(list):
    with open(file_path,"a") as f:
        for li in list:
            s =''
            for l in li:
                s = s + str(l) + ' '
            s += '\n'
            f.writelines(s)
    f.close()

def main():
    preprocessing()
    #将结果写入文件中
    write_txt(find_neighbor())

if __name__ == '__main__':
 main()


